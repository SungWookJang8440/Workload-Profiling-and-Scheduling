"""
Multi-Criteria Decision Making (MCDM) GPU Scheduler
=====================================================
다기준 의사결정 GPU 스케줄링 알고리즘

평가 기준 (4가지):
  1. Performance Score  (S_perf)  : 특정 작업에 성능이 최고인 GPU 대비 정규화된 처리량
  2. Resource Fit Score (S_fit)   : SM/메모리 사용률의 적정 범위 매칭
  3. Cost Efficiency Score (S_cost): 달러당 처리량 정규화
  4. Power Efficiency Score (S_power): 와트당 처리량

가중치: w_perf=0.20, w_fit=0.50, w_cost=0.20, w_power=0.10
"""

from dataclasses import dataclass, field
from typing import Optional
import math


# ─────────────────────────────────────────────
# 데이터 구조 정의
# ─────────────────────────────────────────────

@dataclass
class GPU:
    """GPU 정보"""
    id: str                      # GPU 식별자 (예: "g0", "g1", ...)
    name: str                    # GPU 이름 (예: "NVIDIA H100")
    cost_per_hour: float         # 시간당 비용 (달러)
    tdp_watts: float             # 최대 전력 소비 (W)


@dataclass
class Workload:
    """스케줄링할 작업"""
    id: str                      # 작업 식별자 (예: "w0")
    name: str                    # 작업 이름 (예: "BERT Inference")


@dataclass
class PerformanceMatrix:
    """
    GPUBench 프로파일링 결과 행렬 P ∈ R^{nm}
    P[workload_id][gpu_id] = throughput (samples/sec)
    """
    data: dict[str, dict[str, float]] = field(default_factory=dict)

    def get(self, workload_id: str, gpu_id: str) -> float:
        return self.data.get(workload_id, {}).get(gpu_id, 0.0)

    def throughput_for_workload(self, workload_id: str) -> dict[str, float]:
        """특정 작업에 대한 모든 GPU의 처리량 반환"""
        return self.data.get(workload_id, {})


@dataclass
class SMUtilizationMatrix:
    """
    SM 시용룰 헹랼 (Figure 3 heatmap 실측값)
    data[workload_id][gpu_id] = SM utilization (%)
    """
    data: dict[str, dict[str, float]] = field(default_factory=dict)

    def get(self, workload_id: str, gpu_id: str) -> float:
        return self.data.get(workload_id, {}).get(gpu_id, 0.0)


@dataclass
class MemUtilizationMatrix:
    """
    메모리 사용률 행렬
    data[workload_id][gpu_id] = memory utilization (%)
    논문에 없는 값은 #주석 표기
    """
    data: dict[str, dict[str, float]] = field(default_factory=dict)

    def get(self, workload_id: str, gpu_id: str) -> float:
        return self.data.get(workload_id, {}).get(gpu_id, 0.0)


# ─────────────────────────────────────────────
# 4가지 기준 점수 계산 함수
# ─────────────────────────────────────────────

def score_performance(
    workload: Workload,
    gpu: GPU,
    perf_matrix: PerformanceMatrix,
) -> float:
    """
    S_perf(w_i, g_j) = (P_ij - P_i,min) / (P_i,max - P_i,min) * 100

    특정 작업 w_i에 대해 현재 GPU g_j의 처리량을
    [최소 처리량, 최대 처리량] 범위로 정규화한 점수
    """
    throughputs = perf_matrix.throughput_for_workload(workload.id)
    if not throughputs:
        return 0.0

    p_ij = perf_matrix.get(workload.id, gpu.id)
    p_min = min(throughputs.values())
    p_max = max(throughputs.values())

    if math.isclose(p_max, p_min):
        # 모든 GPU가 동일한 성능이면 100점
        return 100.0

    return (p_ij - p_min) / (p_max - p_min) * 100.0


def _sm_fit_score(u_sm: float) -> float:
    """
    SM 활용률 구간별 점수
      - U_SM < 75%      : (U_SM / 75) * 100
      - 75% ≤ U_SM ≤ 90%: 100
      - U_SM > 90%      : 100 - ((U_SM - 90) / 10) * 10
    """
    if u_sm < 75.0:
        return (u_sm / 75.0) * 100.0
    elif u_sm <= 90.0:
        return 100.0
    else:
        return 100.0 - ((u_sm - 90.0) / 10.0) * 10.0


def _mem_fit_score(u_mem: float) -> float:
    """
    메모리 사용률 구간별 점수 (최적 범위: 55~75%)
    SM 점수와 동일한 구간 구조를 메모리 범위에 적용
      - U_mem < 55%      : (U_mem / 55) * 100
      - 55% ≤ U_mem ≤ 75%: 100
      - U_mem > 75%      : 100 - ((U_mem - 75) / 25) * 100
    """
    if u_mem < 55.0:
        return (u_mem / 55.0) * 100.0
    elif u_mem <= 75.0:
        return 100.0
    else:
        return max(0.0, 100.0 - ((u_mem - 75.0) / 25.0) * 100.0)


def score_resource_fit(
    workload: Workload,
    gpu: GPU,
    sm_matrix: SMUtilizationMatrix,
    mem_matrix: MemUtilizationMatrix,
) -> float:
    """
    S_fit = (S_fit^SM + S_fit^mem) / 2

    SM과 메모리 사용률 각각의 구간별 점수를 평균내어
    자원 적합도 점수를 계산.
    """
    u_sm  = sm_matrix.get(workload.id, gpu.id)
    u_mem = mem_matrix.get(workload.id, gpu.id)
    s_sm  = _sm_fit_score(u_sm)
    s_mem = _mem_fit_score(u_mem)
    
    return (s_sm + s_mem) / 2.0


def score_cost_efficiency(
    workload: Workload,
    gpu: GPU,
    perf_matrix: PerformanceMatrix,
    gpu_list: list,
) -> float:
    """
    S_cost(w_i, g_j) = (P_ij / c_j) / max_{g_k ∈ G}(P_ik / c_k) * 100

    달러당 처리량을 전체 GPU 중 최대값으로 정규화.
    """
    
    # throughput per dollar (달러당 처리량 함수)
    def tpd(g) -> float:
        return perf_matrix.get(workload.id, g.id) / g.cost_per_hour if g.cost_per_hour > 0 else 0.0

    # 현재 GPU의 처리량/비용
    tpd_j = tpd(gpu)

    # 전체 GPU 중 최대 처리량/비용
    max_tpd = max(tpd(g) for g in gpu_list) or 1.0
    return (tpd_j / max_tpd) * 100.0


def score_power_efficiency(
    workload: Workload,
    gpu: GPU,
    perf_matrix: PerformanceMatrix,
    gpu_list: list,
) -> float:
    """
    S_power(w_i, g_j) = P_ij / TDP_j
    
    와트당 처리량 (정규화)
    """
     # throughput per watt (와트당 처리량 함수)
    def tpw(g) -> float:
        return perf_matrix.get(workload.id, g.id) / g.tdp_watts if g.tdp_watts > 0 else 0.0

    tpw_j = tpw(gpu)
    max_tpw = max(tpw(g) for g in gpu_list) or 1.0
    return (tpw_j / max_tpw) * 100.0


# ─────────────────────────────────────────────
# 최종 점수 계산 함수 - MCDM 스케쥴링 알고리즘
# ─────────────────────────────────────────────

@dataclass
class ScoreDetail:
    """한 (workload, GPU) 쌍의 상세 점수"""
    gpu: GPU
    s_perf: float
    s_fit: float
    s_cost: float
    s_power: float
    s_total: float


class MCDMScheduler:
    """
    Multi-Criteria Decision Making GPU Scheduler

    가중치:
      w_perf  = 0.20
      w_fit   = 0.50  ← 자원 매칭을 가장 중요시
      w_cost  = 0.20
      w_power = 0.10
    """

    def __init__(
        self,
        gpus: list[GPU],
        
        perf_matrix: PerformanceMatrix,
        sm_matrix: SMUtilizationMatrix,
        mem_matrix: MemUtilizationMatrix,
        
        w_perf: float = 0.20,
        w_fit: float = 0.5,
        w_cost: float = 0.20,
        w_power: float = 0.10,
    ):
        self.gpus = gpus
        
        self.perf_matrix = perf_matrix
        self.sm_matrix = sm_matrix
        self.mem_matrix = mem_matrix
        
        self.w_perf = w_perf
        self.w_fit = w_fit
        self.w_cost = w_cost
        self.w_power = w_power

    def compute_scores(self, workload: Workload) -> list[ScoreDetail]:
        """
        작업 w_i에 대해 모든 GPU의 4가지 점수와 총점을 계산

        S_total(w_i, g_j) = Σ_{d ∈ {perf,fit,cost,power}} w_d * S_d
        """
        results: list[ScoreDetail] = []

        for gpu in self.gpus:
            s_perf  = score_performance(workload, gpu, self.perf_matrix)
            s_fit = score_resource_fit(workload, gpu, self.sm_matrix, self.mem_matrix)
            s_cost  = score_cost_efficiency(workload, gpu, self.perf_matrix, self.gpus)
            s_power = score_power_efficiency(workload, gpu, self.perf_matrix, self.gpus)

            s_total = (
                self.w_perf  * s_perf  +
                self.w_fit   * s_fit   +
                self.w_cost  * s_cost  +
                self.w_power * s_power
            )

            # 소수점 2자리로 반올림해서 점수 추가
            results.append(ScoreDetail(
                gpu=gpu,
                s_perf=round(s_perf, 2),
                s_fit=round(s_fit, 2),
                s_cost=round(s_cost, 2),
                s_power=round(s_power, 2),
                s_total=round(s_total, 2),
            ))

        return sorted(results, key=lambda x: x.s_total, reverse=True)


    def assign(self, workload: Workload) -> tuple[GPU, list[ScoreDetail]]:
        """
        1. 모든 GPU에 대해 S_total 계산
        2. S_total이 가장 높은 GPU를 작업에 할당
        3. 선택된 GPU와 전체 점수 목록을 반환
        """
        scores = self.compute_scores(workload)
        best = scores[0]
        return best.gpu, scores

    def schedule_all(
        self, workloads: list[Workload]
    ) -> dict[str, tuple[GPU, list[ScoreDetail]]]:
        """
        작업 목록 전체를 순서대로 스케줄링
        반환값: {workload_id: (assigned_gpu, score_details)}
        """
        assignments = {}
        for w in workloads:
            gpu, scores = self.assign(w)
            assignments[w.id] = (gpu, scores)
        return assignments


# ─────────────────────────────────────────────
# 결과 출력 유틸리티
# ─────────────────────────────────────────────

def print_schedule_result(
    workload: Workload,
    assigned_gpu: GPU,
    scores: list[ScoreDetail],
    show_all: bool = True,
) -> None:
    sep = "─" * 72

    print(f"\n{'═'*72}")
    print(f"  작업: [{workload.id}] {workload.name}")
    print(f"{'═'*72}")
    print(f"  {'GPU':<22} {'S_perf':>8} {'S_fit':>8} {'S_cost':>8} {'S_power':>8} {'S_total':>8}")
    print(sep)

    for sd in scores:
        marker = "★" if sd.gpu.id == assigned_gpu.id else " "
        print(
            f"{marker} {sd.gpu.name:<22} "
            f"{sd.s_perf:>8.1f} "
            f"{sd.s_fit:>8.1f} "
            f"{sd.s_cost:>8.1f} "
            f"{sd.s_power:>8.1f} "
            f"{sd.s_total:>8.1f}"
        )

    print(sep)
    print(f"  ✔  할당 GPU: {assigned_gpu.name}  (총점: {scores[0].s_total:.1f})")
    print()


# ─────────────────────────────────────────────
# 데모 실행
# ─────────────────────────────────────────────

def main():
    print("=" * 72)
    print("  MCDM GPU Scheduler  :  다기준 의사결정 GPU 스케줄링 알고리즘")
    print("  가중치: w_perf=0.20 | w_fit=0.50 | w_cost=0.20 | w_power=0.10")
    print("=" * 72)

    # ── GPU 클러스터 정의 ──────────────────────────────────────────
    # 출처: Table II (GPU Hardware Specifications)
    # cost: Table II / tdp_watts: 논문에 데이터 없어서 임의로
    
    gpus = [
        GPU(id="g0", name="RTX 3090",
            cost_per_hour=1.00,   # Table II
            tdp_watts=350),        # 논문에 데이터 없어서 임의로

        GPU(id="g1", name="RTX 4090",
            cost_per_hour=2.50,   # Table II
            tdp_watts=450),        # 논문에 데이터 없어서 임의로 

        GPU(id="g2", name="A100-80GB",
            cost_per_hour=3.00,   # Table II
            tdp_watts=400),        # 논문에 데이터 없어서 임의로

        GPU(id="g3", name="H200",
            cost_per_hour=6.00,   # Table II
            tdp_watts=700),        # 논문에 데이터 없어서 임의로

        GPU(id="g4", name="RTX 6000",
            cost_per_hour=5.00,   # Table II
            tdp_watts=300),        # 논문에 데이터 없어서 임의로
    ]

    # ── 작업 목록 ──────────────────────────────────────────────────
    # 출처: Table I (Workload Suite Coverage) — 논문에 존재하는 workload만 사용
    
    workloads = [
        Workload(id="w0", name="BERT-Base Inference (batch16)"),
        Workload(id="w1", name="BERT-Base Train (batch16)"),
        Workload(id="w2", name="ResNet-50 Train (batch128)"),
    ]

    # ── GPUBench 처리량 행렬 (samples/sec) ────────────────────────
    # throughput = cost_efficiency * cost_per_hour
    # 출처: Table IV (cost efficiency) ,TAable II (GPU별 cost_per_hour)
    
    perf_matrix = PerformanceMatrix(data={
        "w0": {  # BERT-Base Inference batch16 — Table IV
            "g0": 147.0 * 1.00,   # RTX 3090  → 147.0  samples/s
            "g1": 98.3  * 2.50,   # RTX 4090  → 245.75 samples/s
            "g2": 48.7  * 3.00,   # A100      → 146.1  samples/s
            "g3": 66.3  * 6.00,   # H200      → 397.8  samples/s
            "g4": 85.0  * 5.00,   # RTX 6000  → 425.0  samples/s
        },
        "w1": {  # BERT-Base Train batch16 — Table IV
            "g0": 49.9  * 1.00,   # RTX 3090  → 49.9   samples/s
            "g1": 33.0  * 2.50,   # RTX 4090  → 82.5   samples/s
            "g2": 15.6  * 3.00,   # A100      → 46.8   samples/s
            "g3": 21.9  * 6.00,   # H200      → 131.4  samples/s
            "g4": 30.3  * 5.00,   # RTX 6000  → 151.5  samples/s
        },
        "w2": {  # ResNet-50 Train batch128 — Table IV
            "g0": 5408.7 * 1.00,  # RTX 3090  → 5408.7 samples/s
            "g1": 2122.1 * 2.50,  # RTX 4090  → 5305.25 samples/s
            "g2": 1903.8 * 3.00,  # A100      → 5711.4  samples/s
            "g3": 697.0  * 6.00,  # H200      → 4182.0  samples/s
            "g4": 1520.7 * 5.00,  # RTX 6000  → 7603.5  samples/s
        },
    })
    
    # ── SM 사용률 / 메모리 사용률 행렬 ──────────────────────────────
    # 출처: Figure 3 heatmap
    sm_matrix = SMUtilizationMatrix(data={
        "w0": {  # BERT-Base Inf b16
            "g0": 23.2,  # RTX 3090
            "g1": 48.5,  # RTX 4090
            "g2": 41.4,  # A100
            "g3": 14.3,  # H200
            "g4": 2.9,   # RTX 6000
        },
        "w1": {  # BERT-Base Train b16
            "g0": 39.5,  # RTX 3090
            "g1": 80.5,  # RTX 4090
            "g2": 84.8,  # A100
            "g3": 64.2,  # H200
            "g4": 55.5,  # RTX 6000
        },
        "w2": {  # ResNet-50 Train b128
            "g0": 34.3,  # RTX 3090
            "g1": 58.6,  # RTX 4090
            "g2": 54.6,  # A100
            "g3": 20.7,  # H200
            "g4": 41.5,  # RTX 6000
        },
    })
    
    # 논문에 데이터 없어서 임의로
    mem_matrix = MemUtilizationMatrix(data={
        "w0": {  # BERT-Base Inf b16
            "g0": 15.0,
            "g1": 20.0,
            "g2": 11.3,   # V-C절 평균값 사용
            "g3": 4.9,    # V-C절 평균값 사용
            "g4": 10.0,
        },
        "w1": {  # BERT-Base Train b16
            "g0": 15.0,
            "g1": 20.0,
            "g2": 11.3,   # V-C절 평균값 사용
            "g3": 4.9,    # V-C절 평균값 사용
            "g4": 10.0,
        },
        "w2": {  # ResNet-50 Train b128
            "g0": 15.0,
            "g1": 20.0,
            "g2": 11.3,   # V-C절 평균값 사용
            "g3": 4.9,    # V-C절 평균값 사용
            "g4": 10.0,
        },
    })

    #  ── 스케줄러 초기화 및 실행 ────────────────────────────────────
    scheduler = MCDMScheduler(
        gpus=gpus,
        perf_matrix=perf_matrix,
        sm_matrix=sm_matrix,
        mem_matrix=mem_matrix,
    )
    
    assignments = scheduler.schedule_all(workloads)

    for w in workloads:
        assigned_gpu, scores = assignments[w.id]
        print_schedule_result(w, assigned_gpu, scores)

    # ── 최종 요약 ──────────────────────────────────────────────────
    print("=" * 72)
    print("  최종 스케줄링 결과 요약")
    print("=" * 72)
    for w in workloads:
        assigned_gpu, scores = assignments[w.id]
        print(f"  [{w.id}] {w.name:<30} → {assigned_gpu.name}  (총점: {scores[0].s_total:.1f})")
    print("=" * 72)


if __name__ == "__main__":
    main()
