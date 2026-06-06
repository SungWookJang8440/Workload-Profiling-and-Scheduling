import { useEffect, useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Cpu,
  Play,
  Pause,
  RotateCcw,
  Send,
  Clock,
  ChevronRight,
  TrendingUp,
  Terminal,
  Activity,
  CheckCircle2,
  AlertTriangle,
  Info,
  Layers,
  ArrowRight,
  HelpCircle,
  HelpCircle as QuestionIcon
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { api } from '@/lib/api';
import { toast } from '@/lib/toast';

interface QueuedJob {
  jobId: string;
  workloadId: string;
  workloadName: string;
  duration: number;
  remainingTime: number;
  submittedAt: string;
}

interface GpuQueueState {
  gpuId: string;
  gpuName: string;
  queue: QueuedJob[];
  totalPendingTime: number;
}

interface McdmScoreDetail {
  gpu: {
    id: string;
    name: string;
  };
  sfit: number;
  sperf: number;
  scost: number;
  spower: number;
  stotal: number;
}

interface TimeMetric {
  tte: number;
  ttc: number;
  total: number;
  eta: string;
}

interface SubmitResult {
  job_id: string;
  workload_id: string;
  workload_name: string;
  recommended_gpu: string;
  recommended_gpu_id: string;
  mcdm_scores: McdmScoreDetail[];
  time_metrics: { [key: string]: TimeMetric };
  decision_log: string;
  bypassed: boolean;
}

const workloadPresets = [
  { id: 'w0', label: 'ResNet50 Train (batch 32)', prompt: 'ResNet50 모델 학습하고 싶어. 배치 크기는 32로 해줘.' },
  { id: 'w3', label: 'BERT Train (batch 8)', prompt: 'BERT 모델 학습 돌려줘. 배치 사이즈는 8로 부탁해.' },
  { id: 'w6', label: 'Whisper Inf (batch 4)', prompt: 'openai whisper inference 수행해줘. batch size는 4야.' },
  { id: 'w10', label: 'MobileNet Inf (batch 32)', prompt: 'MobileNet 추론 작업 돌려줘. 배치는 32로 설정해줘.' },
];

export function SchedulerPage() {
  const [prompt, setPrompt] = useState('');
  const [loading, setLoading] = useState(false);
  const [gpuStates, setGpuStates] = useState<GpuQueueState[]>([]);
  const [logs, setLogs] = useState<string[]>([]);
  const [isAutoTicking, setIsAutoTicking] = useState(true);
  const [lastResult, setLastResult] = useState<SubmitResult | null>(null);
  const consoleEndRef = useRef<HTMLDivElement>(null);
  const autoTickIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Fetch status of the queues
  const fetchStatus = async (quiet = false) => {
    try {
      const data = await api.getSchedulerStatus();
      setGpuStates(data.gpu_states);
      setLogs(data.logs);
    } catch (error) {
      if (!quiet) {
        toast.error('상태 로드 실패', '대기열 정보를 가져오지 못했습니다.');
      }
    }
  };

  // Submit workload prompt
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) {
      toast.error('입력 오류', '프롬프트를 입력하세요.');
      return;
    }

    setLoading(true);
    try {
      const result = await api.submitSchedulerJob(prompt);
      setLastResult(result);
      toast.success(
        '작업 스케줄링 완료',
        `${result.recommended_gpu} 노드에 작업이 정상 할당되었습니다.`
      );
      setPrompt('');
      await fetchStatus(true);
    } catch (error: any) {
      toast.error(
        '스케줄링 실패',
        error.response?.data?.error || '알고리즘 연산 중 에러가 발생했습니다.'
      );
    } finally {
      setLoading(false);
    }
  };

  // Trigger manual tick
  const handleTick = async () => {
    try {
      const data = await api.tickScheduler();
      setGpuStates(data.gpu_states);
      setLogs(data.logs);
    } catch (error) {
      toast.error('오류', '시뮬레이션 진행을 처리하지 못했습니다.');
    }
  };

  // Reset queues
  const handleReset = async () => {
    try {
      const data = await api.resetScheduler();
      setGpuStates(data.gpu_states);
      setLogs(data.logs);
      setLastResult(null);
      toast.success('초기화 완료', '모든 노드 대기열 및 로그가 초기화되었습니다.');
    } catch (error) {
      toast.error('초기화 실패', '시뮬레이션을 초기화하지 못했습니다.');
    }
  };

  // Select a preset workload
  const handlePresetSelect = (preset: typeof workloadPresets[0]) => {
    setPrompt(preset.prompt);
  };

  // Scroll terminal to the bottom
  useEffect(() => {
    consoleEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  // Initial fetch and auto-tick loop
  useEffect(() => {
    fetchStatus();

    if (isAutoTicking) {
      autoTickIntervalRef.current = setInterval(() => {
        handleTick();
      }, 1000);
    }

    return () => {
      if (autoTickIntervalRef.current) {
        clearInterval(autoTickIntervalRef.current);
      }
    };
  }, [isAutoTicking]);

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold gradient-text flex items-center gap-2">
            <Cpu className="w-8 h-8 text-primary animate-pulse" /> MCDM GPU 스케줄러 및 부하 분산
          </h1>
          <p className="text-muted-foreground mt-1">
            다중 기준 의사결정(MCDM) 알고리즘과 대기열 지연(TTE)을 분석하여 최적의 GPU 노드를 매핑하고 로드를 동적 분산합니다.
          </p>
        </div>
        <div className="flex items-center gap-2 bg-secondary/30 p-1.5 rounded-lg border border-border">
          <Button
            size="sm"
            variant={isAutoTicking ? 'default' : 'ghost'}
            className={isAutoTicking ? 'bg-primary/20 text-primary border border-primary/30 hover:bg-primary/30' : ''}
            onClick={() => setIsAutoTicking(true)}
          >
            <Play className="w-3.5 h-3.5 mr-1.5" /> 자동
          </Button>
          <Button
            size="sm"
            variant={!isAutoTicking ? 'default' : 'ghost'}
            className={!isAutoTicking ? 'bg-amber-500/20 text-amber-500 border border-amber-500/30 hover:bg-amber-500/30' : ''}
            onClick={() => setIsAutoTicking(false)}
          >
            <Pause className="w-3.5 h-3.5 mr-1.5" /> 일시정지
          </Button>
          <div className="h-4 w-px bg-border mx-1" />
          <Button size="sm" variant="outline" onClick={handleTick} disabled={isAutoTicking}>
            1초 진행
          </Button>
          <Button size="sm" variant="destructive" onClick={handleReset}>
            <RotateCcw className="w-3.5 h-3.5 mr-1.5" /> 초기화
          </Button>
        </div>
      </div>

      {/* Grid: Submit + Last Result Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Side: Submit Form */}
        <div className="lg:col-span-6 space-y-6">
          <Card className="gradient-border glass">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Layers className="w-5 h-5 text-primary" /> 워크로드 요청 생성
              </CardTitle>
              <CardDescription>
                인공지능 모델 훈련 및 추론 워크로드를 자연어 명령어로 입력하세요.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-2">
                  <textarea
                    className="w-full h-32 bg-secondary/30 border border-border rounded-lg p-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 text-foreground resize-none"
                    placeholder="예: ResNet50 모델을 배치 크기 32로 에포크 10만큼 돌려줘. 또는 whisper 모델 추론 실행해줘."
                    value={prompt}
                    onChange={(e) => setPrompt(e.target.value)}
                    disabled={loading}
                  />
                </div>
                
                <div className="flex flex-wrap gap-2">
                  {workloadPresets.map((preset) => (
                    <button
                      key={preset.id}
                      type="button"
                      onClick={() => handlePresetSelect(preset)}
                      className="px-2.5 py-1 text-xs rounded-full bg-secondary/40 border border-border text-muted-foreground hover:text-foreground hover:bg-secondary/80 transition-colors"
                    >
                      {preset.label}
                    </button>
                  ))}
                </div>

                <Button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-gradient-to-r from-purple-600 to-blue-600 hover:opacity-90"
                >
                  {loading ? (
                    <span className="flex items-center gap-2">
                      <Activity className="w-4 h-4 animate-spin" /> 스케줄러 분석 중...
                    </span>
                  ) : (
                    <span className="flex items-center gap-2">
                      <Send className="w-4 h-4" /> 분석 및 스케줄링 실행
                    </span>
                  )}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>

        {/* Right Side: Last Recommendation Results */}
        <div className="lg:col-span-6">
          <Card className="gradient-border glass h-full flex flex-col justify-between">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2">
                  <TrendingUp className="w-5 h-5 text-blue-400" /> 스케줄링 의사결정 결과
                </CardTitle>
                {lastResult && (
                  <Badge variant={lastResult.bypassed ? 'warning' : 'success'}>
                    {lastResult.bypassed ? '동적 우회(Bypassed) ⚖️' : 'MCDM 매핑 완료'}
                  </Badge>
                )}
              </div>
              <CardDescription>
                가장 최근 접수된 작업의 실시간 할당 분석 보고서입니다.
              </CardDescription>
            </CardHeader>
            <CardContent className="flex-1 flex flex-col justify-center">
              {lastResult ? (
                <div className="space-y-4">
                  {/* Title of Job & target */}
                  <div className="p-3 bg-secondary/20 rounded-lg border border-border flex items-center justify-between">
                    <div>
                      <p className="text-xs text-muted-foreground">워크로드명 (매핑 ID)</p>
                      <h4 className="font-semibold text-sm">{lastResult.workload_name} ({lastResult.workload_id})</h4>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-muted-foreground">최종 라우팅 GPU</p>
                      <span className="inline-block px-2.5 py-1 text-xs font-bold text-white bg-purple-500 rounded">
                        {lastResult.recommended_gpu}
                      </span>
                    </div>
                  </div>

                  {/* Estimates Comparison */}
                  <div className="grid grid-cols-3 gap-3">
                    {Object.entries(lastResult.time_metrics).map(([gpuId, metric]) => {
                      const isSelected = lastResult.recommended_gpu_id === gpuId;
                      const isMcdmBest = lastResult.mcdm_scores[0].gpu.id === gpuId;
                      const gpuName = gpuId === 'g0' ? 'RTX 3090' : gpuId === 'g1' ? 'RTX 4090' : 'RTX 6000';

                      return (
                        <div
                          key={gpuId}
                          className={`p-2.5 rounded-lg border text-center transition-all ${
                            isSelected
                              ? 'bg-primary/10 border-primary/80 ring-1 ring-primary/40'
                              : 'bg-secondary/10 border-border'
                          }`}
                        >
                          <p className="text-xs font-bold truncate mb-1">
                            {gpuName}
                            {isMcdmBest && <span className="text-[10px] block text-purple-400 font-medium">(MCDM 1위)</span>}
                          </p>
                          <div className="space-y-1 text-left text-[11px] mt-2 border-t border-border/40 pt-1.5">
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">TTE (대기):</span>
                              <span className="font-medium">{metric.tte}s</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">TTC (소요):</span>
                              <span className="font-medium text-emerald-400">{metric.ttc}s</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">ETA (완료):</span>
                              <span className="font-semibold">{metric.eta}</span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Bypass details */}
                  {lastResult.bypassed && (
                    <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg flex gap-2.5 text-xs text-amber-200">
                      <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                      <div>
                        <strong>혼잡 우회 발생:</strong> 스케줄러가 MCDM 1순위 노드 대신 대기 대기열이 짧은 노드를 우회 할당하여 완료 시간을 단축했습니다.
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="text-center py-8 text-muted-foreground space-y-2">
                  <Info className="w-8 h-8 text-muted-foreground/60 mx-auto" />
                  <p className="text-sm">현재 입력된 워크로드가 없습니다.</p>
                  <p className="text-xs">상단의 워크로드 생성기를 사용해 작업을 접수해주세요.</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* GPU Timelines (Gantt Charts) */}
      <Card className="gradient-border glass">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Activity className="w-5 h-5 text-purple-400" /> GPU 대기열 타임라인 (Gantt Chart)
              </CardTitle>
              <CardDescription>
                각 노드별 현재 실행 중인 작업과 대기 중인 큐(Queue)를 실시간 렌더링합니다.
              </CardDescription>
            </div>
            <div className="text-right">
              <span className="inline-block w-3 h-3 bg-primary rounded-full animate-ping mr-2" />
              <span className="text-xs text-muted-foreground">실시간 동기화 활성</span>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          {gpuStates.map((state) => {
            const queue = state.queue || [];
            const hasJobs = queue.length > 0;
            const activeJob = hasJobs ? queue[0] : null;
            const waitingJobs = hasJobs ? queue.slice(1) : [];

            // Hardware details
            let specs = '';
            let share = '';
            if (state.gpuId === 'g0') { specs = 'VRAM 24GB | SM 82 | 전력 350W'; share = '스케줄 지분 6%'; }
            else if (state.gpuId === 'g1') { specs = 'VRAM 24GB | SM 128 | 전력 450W'; share = '스케줄 지분 22%'; }
            else { specs = 'VRAM 48GB | SM 142 | 전력 300W'; share = '스케줄 지분 72%'; }

            return (
              <div key={state.gpuId} className="space-y-2 p-3 bg-secondary/15 rounded-lg border border-border">
                {/* GPU Info Row */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1.5">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-purple-500" />
                    <span className="font-bold text-sm text-foreground">{state.gpuName}</span>
                    <span className="text-[11px] text-muted-foreground">({specs})</span>
                  </div>
                  <div className="flex items-center gap-3 text-xs">
                    <span className="text-muted-foreground font-semibold text-[11px] bg-secondary/40 px-2 py-0.5 rounded border border-border/40">
                      {share}
                    </span>
                    <span className="text-muted-foreground">
                      총 잔여: <span className="font-semibold text-white">{Math.round(state.totalPendingTime)}초</span>
                    </span>
                  </div>
                </div>

                {/* Timeline rendering block */}
                <div className="flex items-center gap-2 min-h-16 overflow-x-auto py-2 pr-2 scrollbar-thin">
                  {activeJob ? (
                    <div className="flex items-center gap-2 shrink-0">
                      {/* Active Job Block */}
                      <div className="relative w-64 bg-primary/20 border border-primary/60 rounded-lg p-2.5 text-xs shadow-md glow overflow-hidden">
                        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent animate-shimmer pointer-events-none" />
                        <div className="flex justify-between items-center mb-1">
                          <span className="font-bold text-white truncate max-w-[130px]">
                            {activeJob.workloadName}
                          </span>
                          <Badge className="bg-primary hover:bg-primary/90 text-[10px] px-1.5 py-0">RUNNING</Badge>
                        </div>
                        <p className="text-[10px] text-muted-foreground mb-2">ID: {activeJob.jobId} (시작 {activeJob.submittedAt})</p>
                        
                        {/* Progress Bar */}
                        <div className="space-y-1">
                          <div className="w-full bg-black/40 h-2 rounded-full overflow-hidden">
                            <motion.div
                              className="bg-gradient-to-r from-purple-500 to-blue-500 h-full"
                              initial={false}
                              animate={{ width: `${Math.max(0, Math.min(100, (activeJob.remainingTime / activeJob.duration) * 100))}%` }}
                              transition={{ duration: 0.8, ease: 'easeOut' }}
                            />
                          </div>
                          <div className="flex justify-between text-[9px] text-muted-foreground">
                            <span>소요: {Math.round(activeJob.duration)}초</span>
                            <span className="font-bold text-white text-[10px]">남음: {Math.round(activeJob.remainingTime)}초</span>
                          </div>
                        </div>
                      </div>

                      {/* Connection arrow to waiting queue */}
                      {waitingJobs.length > 0 && <ChevronRight className="w-5 h-5 text-muted-foreground/60 shrink-0" />}
                    </div>
                  ) : (
                    <div className="w-full bg-secondary/10 border border-dashed border-border rounded-lg py-5 text-center text-xs text-muted-foreground">
                      대기 중인 작업이 없습니다 (IDLE)
                    </div>
                  )}

                  {/* Waiting Queue Blocks */}
                  {waitingJobs.map((job, idx) => (
                    <div key={job.jobId} className="flex items-center gap-2 shrink-0">
                      <div className="w-48 bg-secondary/25 border border-border rounded-lg p-2.5 text-xs hover:border-muted-foreground/50 transition-all">
                        <div className="flex justify-between items-center mb-1">
                          <span className="font-semibold text-muted-foreground truncate max-w-[100px]">
                            {job.workloadName}
                          </span>
                          <span className="text-[9px] text-muted-foreground font-semibold bg-secondary/60 px-1 py-px rounded">
                            대기 {idx + 1}
                          </span>
                        </div>
                        <p className="text-[9px] text-muted-foreground/60 mb-2">ID: {job.jobId}</p>
                        <div className="flex justify-between text-[10px] text-muted-foreground/80 mt-1 border-t border-border/30 pt-1.5">
                          <span>소요 예상:</span>
                          <span className="font-semibold text-white">{Math.round(job.duration)}초</span>
                        </div>
                      </div>
                      {idx < waitingJobs.length - 1 && <ChevronRight className="w-5 h-5 text-muted-foreground/60 shrink-0" />}
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </CardContent>
      </Card>

      {/* MCDM Score Breakdown & Radar Analogue */}
      {lastResult && (
        <Card className="gradient-border glass">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Info className="w-5 h-5 text-purple-400" /> MCDM 의사결정 수치 상세 분석
            </CardTitle>
            <CardDescription>
              알고리즘 가중치(Fit: 60%, Perf: 15%, Cost: 15%, Power: 10%)가 각 노드별 지표에 대입된 계산 세부 내역입니다.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {lastResult.mcdm_scores.map((score) => {
                const isSelected = lastResult.recommended_gpu_id === score.gpu.id;
                
                return (
                  <div
                    key={score.gpu.id}
                    className={`p-4 rounded-lg border space-y-3.5 ${
                      isSelected
                        ? 'bg-primary/5 border-primary/60 ring-1 ring-primary/30'
                        : 'bg-secondary/10 border-border'
                    }`}
                  >
                    <div className="flex justify-between items-center">
                      <h4 className="font-bold text-sm flex items-center gap-1.5">
                        <span className="w-2 h-2 bg-purple-500 rounded-full" />
                        {score.gpu.name}
                      </h4>
                      <Badge className="bg-primary/20 text-primary border border-primary/30 text-[11px] font-bold">
                        총점: {Math.round(score.stotal)}점
                      </Badge>
                    </div>

                    <div className="space-y-2 text-xs">
                      {/* Fit */}
                      <div className="space-y-1">
                        <div className="flex justify-between text-[11px]">
                          <span className="text-muted-foreground">자원 적합도 (Fit) - 60% 가중치</span>
                          <span className="font-semibold text-white">{Math.round(score.sfit)}점</span>
                        </div>
                        <div className="w-full bg-black/40 h-1.5 rounded-full overflow-hidden">
                          <div className="bg-purple-500 h-full" style={{ width: `${score.sfit}%` }} />
                        </div>
                      </div>

                      {/* Performance */}
                      <div className="space-y-1">
                        <div className="flex justify-between text-[11px]">
                          <span className="text-muted-foreground">성능 가중치 (Perf) - 15% 가중치</span>
                          <span className="font-semibold text-white">{Math.round(score.sperf)}점</span>
                        </div>
                        <div className="w-full bg-black/40 h-1.5 rounded-full overflow-hidden">
                          <div className="bg-blue-500 h-full" style={{ width: `${score.sperf}%` }} />
                        </div>
                      </div>

                      {/* Cost */}
                      <div className="space-y-1">
                        <div className="flex justify-between text-[11px]">
                          <span className="text-muted-foreground">비용 효율성 (Cost) - 15% 가중치</span>
                          <span className="font-semibold text-white">{Math.round(score.scost)}점</span>
                        </div>
                        <div className="w-full bg-black/40 h-1.5 rounded-full overflow-hidden">
                          <div className="bg-emerald-500 h-full" style={{ width: `${score.scost}%` }} />
                        </div>
                      </div>

                      {/* Power */}
                      <div className="space-y-1">
                        <div className="flex justify-between text-[11px]">
                          <span className="text-muted-foreground">전력 효율성 (Power) - 10% 가중치</span>
                          <span className="font-semibold text-white">{Math.round(score.spower)}점</span>
                        </div>
                        <div className="w-full bg-black/40 h-1.5 rounded-full overflow-hidden">
                          <div className="bg-orange-500 h-full" style={{ width: `${score.spower}%` }} />
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Terminal Log Console */}
      <Card className="gradient-border glass">
        <CardHeader className="pb-2">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base font-bold flex items-center gap-2">
              <Terminal className="w-4 h-4 text-emerald-400" /> 스케줄러 핵심 이벤트 콘솔 로그
            </CardTitle>
            <span className="text-xs text-muted-foreground bg-secondary/50 px-2 py-0.5 rounded">
              Total logs: {logs.length}
            </span>
          </div>
        </CardHeader>
        <CardContent>
          <div className="bg-black/80 font-mono text-[11px] leading-relaxed p-4 rounded-lg border border-border h-64 overflow-y-auto space-y-1.5 text-emerald-500/90 shadow-inner scrollbar-thin">
            {logs.map((log, index) => {
              // Custom text color for alerts, bypass, status pop
              let logColor = 'text-emerald-500/90';
              if (log.includes('우회') || log.includes('Bypass')) {
                logColor = 'text-amber-400';
              } else if (log.includes('완료 처리') || log.includes('Pop')) {
                logColor = 'text-sky-400';
              } else if (log.includes('초기화')) {
                logColor = 'text-purple-400';
              }

              return (
                <div key={index} className={`${logColor} hover:bg-white/5 px-1 rounded transition-colors`}>
                  {log}
                </div>
              );
            })}
            {logs.length === 0 && (
              <div className="text-muted-foreground text-center py-12">
                출력할 로그 데이터가 없습니다.
              </div>
            )}
            <div ref={consoleEndRef} />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default SchedulerPage;
