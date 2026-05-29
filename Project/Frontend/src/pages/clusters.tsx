import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Server,
  Plus,
  Monitor,
  Cpu,
  HardDrive,
  Activity,
  CheckCircle2,
  XCircle,
  Clock,
  Loader2,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useClusterStore } from '@/lib/store';
import { toast } from '@/lib/toast';

function ClusterCard({ cluster }: { cluster: any }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <Card className="gradient-border h-full">
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <Server className="w-5 h-5 text-primary" />
              </div>
              <div>
                <CardTitle className="text-lg">{cluster.name}</CardTitle>
                <CardDescription className="text-xs">
                  {cluster.ip_address}
                </CardDescription>
              </div>
            </div>
            <Badge variant={cluster.is_active ? 'success' : 'secondary'}>
              {cluster.is_active ? (
                <span className="flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" />
                  활성
                </span>
              ) : (
                <span className="flex items-center gap-1">
                  <XCircle className="w-3 h-3" />
                  비활성
                </span>
              )}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Cpu className="w-4 h-4" />
                GPU
              </div>
              <p className="text-sm font-medium">
                {cluster.gpu_name || 'N/A'} x{cluster.gpu_count}
              </p>
            </div>
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <HardDrive className="w-4 h-4" />
                VRAM
              </div>
              <p className="text-sm font-medium">
                {cluster.gpu_vram_gb ? `${cluster.gpu_vram_gb} GB` : 'N/A'}
              </p>
            </div>
          </div>

          <div className="space-y-1">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Activity className="w-4 h-4" />
              상태
            </div>
            <p className="text-sm font-medium">{cluster.status}</p>
          </div>

          {cluster.description && (
            <p className="text-sm text-muted-foreground line-clamp-2">
              {cluster.description}
            </p>
          )}

          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Clock className="w-3 h-3" />
            등록일: {new Date(cluster.created_at).toLocaleDateString('ko-KR')}
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
}

export function ClustersPage() {
  const { clusters, fetchClusters, addCluster, isLoading } = useClusterStore();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    machineName: '',
    ipAddress: '',
    description: '',
  });

  useEffect(() => {
    fetchClusters();
  }, [fetchClusters]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.machineName.trim()) {
      toast.error('입력 오류', '머신 이름은 필수입니다');
      return;
    }

    setIsSubmitting(true);
    try {
      await addCluster(formData.machineName, formData.ipAddress, formData.description);
      toast.success('클러스터 추가됨', `${formData.machineName} 클러스터가 성공적으로 추가되었습니다`);
      setFormData({ machineName: '', ipAddress: '', description: '' });
      setIsDialogOpen(false);
    } catch (error: any) {
      toast.error('추가 실패', error.response?.data?.message || '클러스터 추가 중 오류가 발생했습니다');
    } finally {
      setIsSubmitting(false);
    }
  };

  const activeClusters = clusters.filter(c => c.is_active).length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold gradient-text">클러스터 관리</h1>
          <p className="text-muted-foreground mt-1">
            GPU 노드 클러스터를 관리하고 모니터링하세요
          </p>
        </div>
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button className="bg-gradient-to-r from-purple-600 to-blue-600">
              <Plus className="w-4 h-4 mr-2" />
              클러스터 추가
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>새 클러스터 추가</DialogTitle>
              <DialogDescription>
                GPU 노드 정보를 입력하여 새로운 클러스터를 등록하세요.
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="machineName">머신 이름 *</Label>
                <Input
                  id="machineName"
                  placeholder="예: gpu-node-01"
                  value={formData.machineName}
                  onChange={(e) => setFormData({ ...formData, machineName: e.target.value })}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="ipAddress">IP 주소</Label>
                <Input
                  id="ipAddress"
                  placeholder="예: 192.168.1.100"
                  value={formData.ipAddress}
                  onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">설명</Label>
                <Input
                  id="description"
                  placeholder="클러스터에 대한 설명"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setIsDialogOpen(false)}>
                  취소
                </Button>
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className="bg-gradient-to-r from-purple-600 to-blue-600"
                >
                  {isSubmitting && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                  추가
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="gradient-border">
          <CardHeader className="pb-2">
            <CardDescription>전체 클러스터</CardDescription>
            <CardTitle className="text-3xl">{clusters.length}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center text-sm text-muted-foreground">
              <Server className="w-4 h-4 mr-2" />
              등록된 GPU 노드
            </div>
          </CardContent>
        </Card>
        <Card className="gradient-border">
          <CardHeader className="pb-2">
            <CardDescription>활성 클러스터</CardDescription>
            <CardTitle className="text-3xl text-green-500">{activeClusters}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center text-sm text-muted-foreground">
              <CheckCircle2 className="w-4 h-4 mr-2 text-green-500" />
              사용 가능
            </div>
          </CardContent>
        </Card>
        <Card className="gradient-border">
          <CardHeader className="pb-2">
            <CardDescription>총 GPU 수</CardDescription>
            <CardTitle className="text-3xl">
              {clusters.reduce((acc, c) => acc + (c.gpu_count || 0), 0)}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center text-sm text-muted-foreground">
              <Monitor className="w-4 h-4 mr-2" />
              가용 GPU
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Clusters Grid */}
      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      ) : clusters.length === 0 ? (
        <Card className="gradient-border">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <div className="w-16 h-16 rounded-full bg-secondary/50 flex items-center justify-center mb-4">
              <Server className="w-8 h-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-semibold mb-2">등록된 클러스터가 없습니다</h3>
            <p className="text-muted-foreground text-center max-w-md mb-4">
              GPU 노드를 추가하여 컨테이너를 배포할 수 있는 클러스터를 구성하세요.
            </p>
            <Button
              onClick={() => setIsDialogOpen(true)}
              className="bg-gradient-to-r from-purple-600 to-blue-600"
            >
              <Plus className="w-4 h-4 mr-2" />
              첫 클러스터 추가
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {clusters.map((cluster) => (
            <ClusterCard key={cluster.id} cluster={cluster} />
          ))}
        </div>
      )}
    </div>
  );
}
