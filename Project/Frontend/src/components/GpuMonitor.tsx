import { useEffect, useState } from 'react';
import { Activity, Server, Thermometer, Zap, Cpu, MemoryStick, Box } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { motion } from 'framer-motion';

interface GpuData {
  uuid: string;
  name: string;
  temperature: string;
  power_draw: string;
  power_limit: string;
  memory_total: string;
  memory_used: string;
  utilization: string;
}

interface MigData {
  uuid: string;
  instance_id: string;
  profile: string;
  memory_total: string;
}

interface GpuMetrics {
  gpu: GpuData;
  mig: MigData[];
}

export function GpuMonitor() {
  const [metrics, setMetrics] = useState<GpuMetrics | null>(null);
  const [status, setStatus] = useState<'CONNECTING' | 'CONNECTED' | 'ERROR'>('CONNECTING');
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    // Vite Proxy: /api/monitoring -> http://localhost:8000/monitoring
    const eventSource = new EventSource('/api/monitoring/gpu-stream');

    eventSource.addEventListener('status', (e) => {
      setStatus(e.data);
    });

    eventSource.addEventListener('gpu_metrics', (e) => {
      try {
        const data = JSON.parse(e.data);
        setMetrics(data);
      } catch (err) {
        console.error("Failed to parse GPU metrics", err);
      }
    });

    eventSource.addEventListener('error', (e) => {
      console.error("SSE Error:", e);
      setStatus('ERROR');
      // @ts-ignore
      setErrorMsg(e.data || 'Connection failed');
    });

    return () => {
      eventSource.close();
    };
  }, []);

  const calculatePercentage = (usedStr: string, totalStr: string) => {
    try {
      const used = parseFloat(usedStr.replace(/[^0-9.]/g, ''));
      const total = parseFloat(totalStr.replace(/[^0-9.]/g, ''));
      if (total === 0) return 0;
      return Math.min(100, Math.round((used / total) * 100));
    } catch {
      return 0;
    }
  };

  const getPowerPercentage = (draw: string, limit: string) => {
    return calculatePercentage(draw, limit);
  };

  return (
    <Card className="gradient-border mb-6 border-zinc-800/50 bg-black/40 backdrop-blur-md">
      <CardHeader className="pb-3 border-b border-zinc-800/50">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-emerald-400" />
            <CardTitle className="text-xl font-bold tracking-tight text-zinc-100">
              실시간 GPU 노드 모니터링
            </CardTitle>
          </div>
          <Badge variant={status === 'CONNECTED' ? 'success' : status === 'ERROR' ? 'destructive' : 'secondary'}
                 className="font-mono text-xs animate-pulse">
            {status === 'CONNECTED' ? 'LIVE' : status}
          </Badge>
        </div>
      </CardHeader>
      
      <CardContent className="p-0">
        {status === 'ERROR' ? (
          <div className="p-8 text-center text-red-400 font-mono text-sm">
            [연결 오류] 워커 노드(SSH)와 통신할 수 없습니다: {errorMsg}
          </div>
        ) : !metrics ? (
          <div className="p-8 text-center text-zinc-500 font-mono text-sm animate-pulse">
            waiting for nvtop metrics stream...
          </div>
        ) : (
          <div className="flex flex-col">
            {/* Main GPU Stats */}
            <div className="p-5 grid grid-cols-1 lg:grid-cols-4 gap-6 bg-zinc-900/50">
              <div className="lg:col-span-1 space-y-2">
                <div className="text-xs text-zinc-400 font-semibold uppercase tracking-wider flex items-center">
                  <Server className="w-3 h-3 mr-1" />
                  GPU 정보
                </div>
                <div className="text-lg font-bold text-emerald-400">
                  {metrics.gpu.name}
                </div>
                <div className="text-xs font-mono text-zinc-500 truncate">
                  {metrics.gpu.uuid}
                </div>
              </div>
              
              <div className="lg:col-span-3 grid grid-cols-1 md:grid-cols-3 gap-4">
                {/* Temp */}
                <div className="bg-zinc-950/50 p-3 rounded-lg border border-zinc-800/50">
                  <div className="flex items-center gap-2 text-xs text-zinc-400 mb-1">
                    <Thermometer className="w-3 h-3 text-orange-400" />
                    온도
                  </div>
                  <div className="text-xl font-mono">{metrics.gpu.temperature}</div>
                </div>
                
                {/* Power */}
                <div className="bg-zinc-950/50 p-3 rounded-lg border border-zinc-800/50">
                  <div className="flex items-center gap-2 text-xs text-zinc-400 mb-1">
                    <Zap className="w-3 h-3 text-yellow-400" />
                    물리 전력 (Draw / Limit)
                  </div>
                  <div className="text-sm font-mono truncate">{metrics.gpu.power_draw} / {metrics.gpu.power_limit}</div>
                  <div className="mt-2 w-full bg-zinc-800 rounded-full h-1.5">
                    <div className="bg-yellow-400 h-1.5 rounded-full" style={{ width: `${getPowerPercentage(metrics.gpu.power_draw, metrics.gpu.power_limit)}%` }}></div>
                  </div>
                </div>

                {/* VRAM Total */}
                <div className="bg-zinc-950/50 p-3 rounded-lg border border-zinc-800/50">
                  <div className="flex items-center gap-2 text-xs text-zinc-400 mb-1">
                    <MemoryStick className="w-3 h-3 text-purple-400" />
                    물리 VRAM 사용량
                  </div>
                  <div className="text-sm font-mono truncate">{metrics.gpu.memory_used} / {metrics.gpu.memory_total}</div>
                  <div className="mt-2 w-full bg-zinc-800 rounded-full h-1.5">
                    <div className="bg-purple-400 h-1.5 rounded-full transition-all duration-500" style={{ width: `${calculatePercentage(metrics.gpu.memory_used, metrics.gpu.memory_total)}%` }}></div>
                  </div>
                </div>
              </div>
            </div>

            {/* MIG Instances */}
            {metrics.mig && metrics.mig.length > 0 && (
              <div className="p-5 border-t border-zinc-800/50">
                <div className="text-xs text-zinc-400 font-semibold uppercase tracking-wider mb-4 flex items-center">
                  <Box className="w-3 h-3 mr-1" />
                  MIG 인스턴스 격리 현황 ({metrics.mig.length}개)
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {metrics.mig.map((m, idx) => {
                    const isWorkerAllocated = m.profile.includes("24gb"); // 임시 상태 로직
                    return (
                      <motion.div 
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        key={m.uuid}
                        className="bg-zinc-950 p-4 rounded-lg border border-zinc-800 flex flex-col gap-2"
                      >
                        <div className="flex justify-between items-center mb-1">
                          <span className="font-mono text-sm font-bold text-zinc-200">Device {m.instance_id}</span>
                          <Badge variant="outline" className={`text-[10px] py-0 border-zinc-700 ${isWorkerAllocated ? 'bg-purple-500/20 text-purple-300' : 'bg-zinc-800 text-zinc-400'}`}>
                            {m.profile}
                          </Badge>
                        </div>
                        <div className="flex flex-col text-[10px] text-zinc-500 font-mono">
                          <span className="text-zinc-400">UUID</span>
                          <span className="truncate">{m.uuid}</span>
                        </div>
                        <div className="flex justify-between items-end mt-1">
                          <span className="text-xs text-zinc-400">할당 VRAM</span>
                          <span className="text-sm font-mono text-zinc-300">{m.memory_total}</span>
                        </div>
                      </motion.div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
