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
    watts: float             # 최대 전력 소비 (W)


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
    SM 시용룰 헹렬
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
    """
    data: dict[str, dict[str, float]] = field(default_factory=dict)

    def get(self, workload_id: str, gpu_id: str) -> float:
        return self.data.get(workload_id, {}).get(gpu_id, 0.0)


@dataclass
class LatencyMatrix:
    """
    처리 시간 행렬
    data[workload_id][gpu_id] = latency (sec)
    """
    data: dict[str, dict[str, float]] = field(default_factory=dict)

    def get(self, workload_id: str, gpu_id: str) -> float:
        return self.data.get(workload_id, {}).get(gpu_id, 0.0)


# ─────────────────────────────────────────────
# 4가지 기준 점수 계산 함수
# ─────────────────────────────────────────────

'''
# 원래 논문 방식인데 GPU개수가 적으면 각 GPU마다 격차가 너무 크게 나옴
# score_performance 방식은 계산 방식 변경, cost와 power는 sqrt로 정규화하여 격차 완화
# cost와 watts를 제곱해서 사용해서 비용 / 와트 페널티 증가


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
'''

def score_performance(
    workload: Workload,
    gpu: GPU,
    perf_matrix: PerformanceMatrix,
) -> float:
    """
    S_perf(w_i, g_j) = sqrt(P_ij / P_i,max) * 100

    최고 성능 GPU를 기준으로 비선형 정규화 방식을 사용하여 성능 차이가 지나치게 확대되는 것을 방지
    """
    throughputs = perf_matrix.throughput_for_workload(workload.id)
    if not throughputs:
        return 0.0

    p_ij = perf_matrix.get(workload.id, gpu.id)
    p_max = max(throughputs.values())

    if math.isclose(p_max, 0.0):
        return 0.0

    return math.sqrt(p_ij / p_max) * 100.0


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
      - U_mem > 75%      : 100 - ((U_mem - 75) / 25) * 50
    """
    if u_mem < 55.0:
        return (u_mem / 55.0) * 100.0
    elif u_mem <= 75.0:
        return 100.0
    else:
        return max(0.0, 100.0 - ((u_mem - 75.0) / 25.0) * 50.0)


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

    달러당 처리량을 전체 GPU 중 최대값으로 정규화 + 분모 제곱으로 페널티 강화 및 제곱근으로 격차 완화
    """
    
    # throughput per dollar (달러당 처리량 함수)
    def tpd(g) -> float:
        return perf_matrix.get(workload.id, g.id) / (g.cost_per_hour**2) if g.cost_per_hour > 0 else 0.0

    # 현재 GPU의 처리량/비용
    tpd_j = tpd(gpu)

    # 전체 GPU 중 최대 처리량/비용
    max_tpd = max(tpd(g) for g in gpu_list) or 1.0
    return math.sqrt(tpd_j / max_tpd) * 100.0


def score_power_efficiency(
    workload: Workload,
    gpu: GPU,
    perf_matrix: PerformanceMatrix,
    gpu_list: list,
) -> float:
    """
    S_power(w_i, g_j) = (P_ij / watts_j) / max_{g_k ∈ G}(P_ik / watts_k) * 100
    
    와트당 처리량 (정규화) + 분모 제곱으로 페널티 강화 및 제곱근으로 격차 완화
    """
     # throughput per watt (와트당 처리량 함수)
    def tpw(g) -> float:
        return perf_matrix.get(workload.id, g.id) / (g.watts**2) if g.watts > 0 else 0.0

    tpw_j = tpw(gpu)
    max_tpw = max(tpw(g) for g in gpu_list) or 1.0
    return  math.sqrt(tpw_j / max_tpw) * 100.0


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
      w_perf  = 0.15
      w_fit   = 0.60  ← 자원 매칭을 가장 중요시
      w_cost  = 0.15
      w_power = 0.10
    """

    def __init__(
        self,
        gpus: list[GPU],
        
        perf_matrix: PerformanceMatrix,
        sm_matrix: SMUtilizationMatrix,
        mem_matrix: MemUtilizationMatrix,
        sec_matrix: LatencyMatrix,
        
        w_perf: float = 0.15,
        w_fit: float = 0.60,
        w_cost: float = 0.15,
        w_power: float = 0.10,
    ):
        self.gpus = gpus
        
        self.perf_matrix = perf_matrix
        self.sm_matrix = sm_matrix
        self.mem_matrix = mem_matrix
        self.sec_matrix = sec_matrix
        
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
    latency: float,
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
    print(f"  ✔  할당 GPU: {assigned_gpu.name}  (총점: {scores[0].s_total:.1f})  |  처리시간: {latency:.4f} sec")
    print()


# ─────────────────────────────────────────────
# 데이터 초기화 (baseline_merged.csv 실측값)
# ─────────────────────────────────────────────
 
def build_scheduler() -> tuple[MCDMScheduler, list[Workload]]:
    """GPU, 워크로드, 행렬 데이터를 초기화하고 스케줄러를 반환"""
 
    # ── GPU 클러스터 정의 ──────────────────────────────────────────
    # cost_per_hour: Table II / watts: 검색값
    gpus = [
        GPU(id="g0", name="RTX 3090",
            cost_per_hour=1.00,
            watts=350),
        GPU(id="g1", name="RTX 4090",
            cost_per_hour=2.50,
            watts=450),
        GPU(id="g2", name="RTX 6000",
            cost_per_hour=5,
            watts=600),
    ]
 
    # ── 작업 목록 ──────────────────────────────────────────────────
    workloads = [
        Workload(id="w0",  name="resnet50-train (batch32)"),
        Workload(id="w1",  name="resnet50-train (batch64)"),
        Workload(id="w2",  name="resnet50-train (batch128)"),
        Workload(id="w3",  name="bert-base-cased-train (batch8)"),
        Workload(id="w4",  name="bert-base-cased-train (batch16)"),
        Workload(id="w5",  name="bert-base-cased-train (batch32)"),
        Workload(id="w6",  name="openai-whisper-large-v2-inf (batch4)"),
        Workload(id="w7",  name="openai-whisper-large-v2-inf (batch8)"),
        Workload(id="w8",  name="openai-whisper-large-v2-inf (batch16)"),
        Workload(id="w9",  name="google-mobilenet_v2-inf (batch16)"),
        Workload(id="w10", name="google-mobilenet_v2-inf (batch32)"),
        Workload(id="w11", name="google-mobilenet_v2-inf (batch64)"),
        Workload(id="w12", name="google-vit-base-patch16-224-inf (batch8)"),
        Workload(id="w13", name="google-vit-base-patch16-224-inf (batch16)"),
        Workload(id="w14", name="google-vit-base-patch16-224-inf (batch32)"),
        Workload(id="w15", name="bert-base-cased-inf (batch16)"),
        Workload(id="w16", name="bert-base-cased-inf (batch32)"),
        Workload(id="w17", name="bert-base-cased-inf (batch64)"),
    ]
 
    # ── 처리량 행렬 (Exclusive100, samples/sec) ───────────────────
    perf_matrix = PerformanceMatrix(data={
        "w0":  {"g0": 1638.02, "g1": 1430.12, "g2": 1658.52},
        "w1":  {"g0": 3205.87, "g1": 2797.31, "g2": 1491.43},
        "w2":  {"g0": 5408.68, "g1": 5103.74, "g2": 1351.44},
        "w3":  {"g0":   46.83, "g1":   71.64, "g2": 498.11},
        "w4":  {"g0":   49.91, "g1":   83.18, "g2": 676.18},
        "w5":  {"g0":   51.55, "g1":   84.09, "g2": 787.51},
        "w6":  {"g0":    5.40, "g1":    7.25, "g2": 743.15},
        "w7":  {"g0":    5.74, "g1":    9.58, "g2": 907.26},
        "w8":  {"g0":    5.88, "g1":    9.04, "g2": 970.49},
        "w9":  {"g0":  273.20, "g1":  210.35, "g2": 12483.96},
        "w10": {"g0":  253.20, "g1":  214.84, "g2": 13250.14},
        "w11": {"g0":  258.63, "g1":  201.83, "g2": 11641.03},
        "w12": {"g0":  192.82, "g1":  195.48, "g2": 1176.46},
        "w13": {"g0":  196.58, "g1":  197.50, "g2": 1246.63},
        "w14": {"g0":  181.65, "g1":  180.81, "g2": 1254.23},
        "w15": {"g0":  146.98, "g1":  245.91, "g2": 1768.03},
        "w16": {"g0":  150.32, "g1":  244.78, "g2": 1857.49},
        "w17": {"g0":  151.35, "g1":  243.21, "g2": 1897.73},
    })
 
    # ── SM 사용률 행렬 (sm%) ──────────────────────────────────────
    sm_matrix = SMUtilizationMatrix(data={
        "w0":  {"g0": 22.11, "g1": 33.66, "g2": 97.00},
        "w1":  {"g0": 30.46, "g1": 41.68, "g2": 98.77},
        "w2":  {"g0": 34.33, "g1": 56.30, "g2": 99.00},
        "w3":  {"g0": 32.58, "g1": 66.29, "g2": 88.24},
        "w4":  {"g0": 39.49, "g1": 81.67, "g2": 71.46},
        "w5":  {"g0": 44.11, "g1": 88.70, "g2": 90.55},
        "w6":  {"g0": 39.56, "g1": 59.84, "g2": 48.00},
        "w7":  {"g0": 43.75, "g1": 78.38, "g2": 2.00},
        "w8":  {"g0": 46.76, "g1": 87.68, "g2": 91.27},
        "w9":  {"g0":  2.34, "g1":  2.25, "g2": 99.00},
        "w10": {"g0":  3.00, "g1":  3.13, "g2": 99.00},
        "w11": {"g0":  4.45, "g1":  4.08, "g2": 97.91},
        "w12": {"g0":  6.85, "g1":  8.33, "g2": 97.64},
        "w13": {"g0":  8.58, "g1": 12.29, "g2": 98.92},
        "w14": {"g0": 11.33, "g1": 15.04, "g2": 99.90},
        "w15": {"g0": 23.19, "g1": 43.53, "g2": 30.95},
        "w16": {"g0": 32.94, "g1": 65.33, "g2": 98.94},
        "w17": {"g0": 40.23, "g1": 77.59, "g2": 83.82},
    })
 
    # ── 메모리 사용률 행렬 (mem%) ─────────────────────────────────
    mem_matrix = MemUtilizationMatrix(data={
        "w0":  {"g0": 10.00, "g1": 14.64, "g2": 80.36},
        "w1":  {"g0": 11.00, "g1": 15.13, "g2": 88.07},
        "w2":  {"g0": 12.26, "g1": 15.24, "g2": 91.21},
        "w3":  {"g0": 19.34, "g1": 41.12, "g2": 21.42},
        "w4":  {"g0": 21.40, "g1": 56.21, "g2": 18.23},
        "w5":  {"g0": 23.85, "g1": 66.37, "g2": 25.37},
        "w6":  {"g0": 19.58, "g1": 36.04, "g2": 11.00},
        "w7":  {"g0": 19.59, "g1": 45.34, "g2": 0.01},
        "w8":  {"g0": 20.48, "g1": 51.43, "g2": 28.82},
        "w9":  {"g0":  2.09, "g1":  1.13, "g2": 92.00},
        "w10": {"g0":  2.51, "g1":  2.22, "g2": 92.00},
        "w11": {"g0":  3.93, "g1":  3.38, "g2": 87.09},
        "w12": {"g0":  2.94, "g1":  2.33, "g2": 39.93},
        "w13": {"g0":  4.72, "g1":  4.94, "g2": 18.62},
        "w14": {"g0":  5.44, "g1":  7.00, "g2": 18.90},
        "w15": {"g0": 12.17, "g1": 27.80, "g2": 5.37},
        "w16": {"g0": 17.19, "g1": 46.90, "g2": 16.63},
        "w17": {"g0": 21.61, "g1": 52.41, "g2": 18.44},
    })
    
    # ── 처리시간 행렬 (sec) ─────────────────────────────────
    latency_matrix = LatencyMatrix(data={
        "w0":  {"g0": 1.9535, "g1": 2.2376, "g2": 1.9294},
        "w1":  {"g0": 1.9963, "g1": 2.2879, "g2": 4.2911},
        "w2":  {"g0": 2.3665, "g1": 2.5080, "g2": 9.4712},
        "w3":  {"g0": 17.0827,"g1": 11.1672,"g2": 1.6060},
        "w4":  {"g0": 32.0577,"g1": 19.2354,"g2": 2.3660},
        "w5":  {"g0": 62.0757,"g1": 38.0545,"g2": 4.0634},
        "w6":  {"g0": 74.0741,"g1": 55.1724,"g2": 0.5382},
        "w7":  {"g0": 139.3574,"g1": 83.5073,"g2": 0.8817},
        "w8":  {"g0": 272.1088,"g1": 176.9912,"g2": 1.6487},
        "w9":  {"g0": 5.8566, "g1": 7.6065, "g2": 0.1282},
        "w10": {"g0": 12.6374,"g1": 14.8951,"g2": 0.2415},
        "w11": {"g0": 24.7499,"g1": 31.7096,"g2": 0.5497},
        "w12": {"g0": 4.1488, "g1": 4.0924, "g2": 0.6800},
        "w13": {"g0": 8.1393, "g1": 8.1013, "g2": 1.2835},
        "w14": {"g0": 17.6155,"g1": 17.6984,"g2": 2.5516},
        "w15": {"g0": 10.8858,"g1": 6.5065, "g2": 0.9049},
        "w16": {"g0": 21.2882,"g1": 13.0728,"g2": 1.7228},
        "w17": {"g0": 42.2869,"g1": 26.3175,"g2": 3.3724},
    })

 
    scheduler = MCDMScheduler(
        gpus=gpus,
        perf_matrix=perf_matrix,
        sm_matrix=sm_matrix,
        mem_matrix=mem_matrix,
        sec_matrix=latency_matrix,
    )
    return scheduler, workloads

# ─────────────────────────────────────────────
# 인터랙티브 실행
# ─────────────────────────────────────────────
 
def print_workload_list(workloads: list[Workload]) -> None:
    """사용 가능한 작업 목록 출력"""
    print("\n  사용 가능한 작업 목록:")
    print("  " + "─" * 50)
    for w in workloads:
        print(f"  {w.id:<5}  {w.name}")
    print("  " + "─" * 50)


def main():
    print("=" * 72)
    print("  MCDM GPU Scheduler  :  다기준 의사결정 GPU 스케줄링 알고리즘")
    print("  가중치: w_perf=0.15 | w_fit=0.60 | w_cost=0.15 | w_power=0.10")
    print("=" * 72)

    scheduler, workloads = build_scheduler()
    workload_map = {w.id: w for w in workloads}

    print_workload_list(workloads)
    print()
    print("  입력 방법: 작업 ID 하나 입력 또는 전체 출력은 all 입력")
    print("  예시) w0")
    print("  종료) q 또는 quit")
    print()

    while True:
        try:
            raw = input("  입력 > ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            print("\n  종료합니다.")
            break

        if not raw:
            continue

        # 종료
        if raw in ("q", "quit", "exit"):
            print("  종료합니다.")
            break

        # 목록 보기
        if raw in ("list", "ls", "?", "help"):
            print_workload_list(workloads)
            continue

        # 전체 실행
        if raw == "all":
            counter = {}
            for w in workloads:
                assignments = scheduler.schedule_all([w])
                assigned_gpu, scores = assignments[w.id]
                latency = scheduler.sec_matrix.get(w.id, assigned_gpu.id)
                print_schedule_result(w, assigned_gpu, scores, latency)
                counter[assigned_gpu.name] = counter.get(assigned_gpu.name, 0) + 1

            total = len(workloads)
            summary = " | ".join(f"{name}: {cnt/total*100:.0f}%" for name, cnt in counter.items())
            print(f"  GPU 할당 비율 →  {summary}\n")
            continue

        # "w0 비용" 형태 파싱
        parts = raw.split()
        raw_id = parts[0]
        suffix = parts[1] if len(parts) == 2 else ""

        # 허용된 접미사 외 공백 입력 차단
        if len(parts) > 2 or (len(parts) == 2 and suffix != "비용"):
            print("  ⚠ 올바른 형식으로 입력하세요. (예: w3 또는 w3 비용)")
            continue

        # 유효성 검사
        if raw_id not in workload_map:
            print(f"  ⚠ 알 수 없는 작업 ID: {raw_id}")
            print("     사용 가능한 ID: w0 ~ w17")
            continue

        # 단일 작업 실행
        workload = workload_map[raw_id]
        
        if suffix == "비용":
            scheduler.w_perf  = 0.10
            scheduler.w_fit   = 0.50
            scheduler.w_cost  = 0.30
            scheduler.w_power = 0.10
        else:
            scheduler.w_perf  = 0.15
            scheduler.w_fit   = 0.60
            scheduler.w_cost  = 0.15
            scheduler.w_power = 0.10
        
        assignments = scheduler.schedule_all([workload])
        assigned_gpu, scores = assignments[workload.id]
        latency = scheduler.sec_matrix.get(workload.id, assigned_gpu.id)
        print_schedule_result(workload, assigned_gpu, scores, latency)


if __name__ == "__main__":
    main()