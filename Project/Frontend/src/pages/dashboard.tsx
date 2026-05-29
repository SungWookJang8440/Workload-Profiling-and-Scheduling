import { useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Plus,
  Box,
  Clock,
  Terminal,
  Trash2,
  RefreshCw,
  ExternalLink,
  AlertCircle,
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useContainerStore, useTemplateStore, Container } from '@/lib/store';
import { toast } from '@/lib/toast';
import { useState } from 'react';
import { GpuMonitor } from '@/components/GpuMonitor';

function ContainerCard({ container, onDelete }: { container: Container; onDelete: (id: string) => void }) {
  const statusColors = {
    STARTING: 'warning',
    RUNNING: 'success',
    STOPPED: 'secondary',
    ERROR: 'destructive',
  } as const;

  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await onDelete(container.container_id);
      toast.success('컨테이너 삭제됨', `${container.image_name} 컨테이너가 삭제되었습니다`);
    } catch (error) {
      toast.error('삭제 실패', '컨테이너 삭제 중 오류가 발생했습니다');
    } finally {
      setIsDeleting(false);
      setShowDeleteDialog(false);
    }
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    toast.success('복사됨', `${label}가 클립보드에 복사되었습니다`);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <Card className="gradient-border">
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <Box className="w-5 h-5 text-primary" />
              </div>
              <div>
                <CardTitle className="text-lg">{container.image_name}</CardTitle>
                <CardDescription className="text-xs font-mono">
                  {container.container_id.slice(0, 12)}...
                </CardDescription>
              </div>
            </div>
            <Badge variant={statusColors[container.status] || 'default'}>
              {container.status}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Terminal className="w-4 h-4" />
                SSH 포트
              </div>
              <p className="text-sm font-mono">{container.ssh_port_mapped}</p>
            </div>
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Clock className="w-4 h-4" />
                시작 시간
              </div>
              <p className="text-sm">
                {new Date(container.started_at).toLocaleString('ko-KR')}
              </p>
            </div>
          </div>

          {container.ssh_command && (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">SSH 접속</span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => copyToClipboard(container.ssh_command, 'SSH 명령어')}
                >
                  <ExternalLink className="w-4 h-4 mr-1" />
                  복사
                </Button>
              </div>
              <code className="block p-3 rounded-lg bg-secondary/50 text-xs font-mono break-all">
                {container.ssh_command}
              </code>
            </div>
          )}

          {container.ssh_password && (
            <div className="flex items-center justify-between p-3 rounded-lg bg-secondary/30">
              <div>
                <span className="text-sm text-muted-foreground">비밀번호</span>
                <p className="text-sm font-mono">{container.ssh_password}</p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => copyToClipboard(container.ssh_password, '비밀번호')}
              >
                복사
              </Button>
            </div>
          )}

          <div className="flex gap-2">
            <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
              <DialogTrigger asChild>
                <Button variant="destructive" size="sm" className="flex-1">
                  <Trash2 className="w-4 h-4 mr-2" />
                  삭제
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>컨테이너 삭제</DialogTitle>
                  <DialogDescription>
                    {container.image_name} 컨테이너를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
                  </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setShowDeleteDialog(false)}>
                    취소
                  </Button>
                  <Button variant="destructive" onClick={handleDelete} disabled={isDeleting}>
                    {isDeleting && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                    삭제
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
}

export function DashboardPage() {
  const { containers, fetchContainers, isLoading, deleteContainer, reconcileSessions } = useContainerStore();
  const { templates, fetchTemplates } = useTemplateStore();
  const [isCreating, setIsCreating] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  useEffect(() => {
    fetchContainers();
    fetchTemplates();
  }, [fetchContainers, fetchTemplates]);

  const handleCreateContainer = async () => {
    if (!selectedTemplate) return;
    
    setIsCreating(true);
    try {
      const { createContainer } = useContainerStore.getState();
      await createContainer(selectedTemplate);
      toast.success('컨테이너 생성됨', '새로운 컨테이너가 성공적으로 생성되었습니다');
      setCreateDialogOpen(false);
      setSelectedTemplate('');
    } catch (error: any) {
      toast.error('생성 실패', error.response?.data?.message || '컨테이너 생성 중 오류가 발생했습니다');
    } finally {
      setIsCreating(false);
    }
  };

  const handleRefresh = async () => {
    await reconcileSessions();
    toast.success('동기화 완료', '세션 상태가 동기화되었습니다');
  };

  const runningContainers = containers.filter(c => c.status === 'RUNNING').length;
  const totalContainers = containers.length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold gradient-text">대시보드</h1>
          <p className="text-muted-foreground mt-1">
            GPU 리소스 사용 현황을 확인하고 컨테이너를 관리하세요
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={handleRefresh}>
            <RefreshCw className="w-4 h-4 mr-2" />
            동기화
          </Button>
          <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button size="sm" className="bg-gradient-to-r from-purple-600 to-blue-600">
                <Plus className="w-4 h-4 mr-2" />
                새 컨테이너
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>새 컨테이너 생성</DialogTitle>
                <DialogDescription>
                  실행할 Docker 이미지를 선택하세요. 선택한 이미지로 새로운 컨테이너가 생성됩니다.
                </DialogDescription>
              </DialogHeader>
              <div className="py-4">
                <Select value={selectedTemplate} onValueChange={setSelectedTemplate}>
                  <SelectTrigger>
                    <SelectValue placeholder="이미지 선택..." />
                  </SelectTrigger>
                  <SelectContent>
                    {templates.map((template) => (
                      <SelectItem key={template.id} value={template.image_name}>
                        {template.image_name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setCreateDialogOpen(false)}>
                  취소
                </Button>
                <Button
                  onClick={handleCreateContainer}
                  disabled={!selectedTemplate || isCreating}
                  className="bg-gradient-to-r from-purple-600 to-blue-600"
                >
                  {isCreating && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                  생성
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      {/* Real-time GPU Monitor */}
      <GpuMonitor />

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="gradient-border">
          <CardHeader className="pb-2">
            <CardDescription>전체 컨테이너</CardDescription>
            <CardTitle className="text-3xl">{totalContainers}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center text-sm text-muted-foreground">
              <Box className="w-4 h-4 mr-2" />
              활성 세션
            </div>
          </CardContent>
        </Card>
        <Card className="gradient-border">
          <CardHeader className="pb-2">
            <CardDescription>실행 중</CardDescription>
            <CardTitle className="text-3xl text-green-500">{runningContainers}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center text-sm text-muted-foreground">
              <div className="w-2 h-2 rounded-full bg-green-500 mr-2 animate-pulse" />
              현재 실행 중
            </div>
          </CardContent>
        </Card>
        <Card className="gradient-border">
          <CardHeader className="pb-2">
            <CardDescription>사용 가능한 이미지</CardDescription>
            <CardTitle className="text-3xl">{templates.length}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center text-sm text-muted-foreground">
              <Layers className="w-4 h-4 mr-2" />
              템플릿
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Containers Grid */}
      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      ) : containers.length === 0 ? (
        <Card className="gradient-border">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <div className="w-16 h-16 rounded-full bg-secondary/50 flex items-center justify-center mb-4">
              <AlertCircle className="w-8 h-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-semibold mb-2">컨테이너가 없습니다</h3>
            <p className="text-muted-foreground text-center max-w-md mb-4">
              아직 생성된 컨테이너가 없습니다. 새 컨테이너를 만들어 GPU 리소스를 사용해보세요.
            </p>
            <Button
              onClick={() => setCreateDialogOpen(true)}
              className="bg-gradient-to-r from-purple-600 to-blue-600"
            >
              <Plus className="w-4 h-4 mr-2" />
              첫 컨테이너 만들기
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {containers.map((container) => (
            <ContainerCard
              key={container.id}
              container={container}
              onDelete={deleteContainer}
            />
          ))}
        </div>
      )}
    </div>
  );
}

import { Layers } from 'lucide-react';
