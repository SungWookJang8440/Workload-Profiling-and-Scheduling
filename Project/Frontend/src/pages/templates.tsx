import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Layers,
  Plus,
  Package,
  Tag,
  Clock,
  Loader2,
  Copy,
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
import { useTemplateStore, useContainerStore } from '@/lib/store';
import { toast } from '@/lib/toast';

function TemplateCard({ template, onCreate }: { template: any; onCreate: (imageName: string) => void }) {
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const handleCreate = async () => {
    setIsCreating(true);
    try {
      await onCreate(template.image_name);
      toast.success('컨테이너 생성됨', `${template.image_name} 컨테이너가 생성되었습니다`);
      setShowCreateDialog(false);
    } catch (error: any) {
      toast.error('생성 실패', error.response?.data?.message || '컨테이너 생성 중 오류가 발생했습니다');
    } finally {
      setIsCreating(false);
    }
  };

  const copyImageName = () => {
    navigator.clipboard.writeText(template.image_name);
    toast.success('복사됨', '이미지 이름이 클립보드에 복사되었습니다');
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
                <Package className="w-5 h-5 text-primary" />
              </div>
              <div className="flex-1 min-w-0">
                <CardTitle className="text-sm truncate" title={template.image_name}>
                  {template.image_name}
                </CardTitle>
                <CardDescription className="text-xs">
                  ID: {template.id}
                </CardDescription>
              </div>
            </div>
            <Badge variant="default">Docker</Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Clock className="w-3 h-3" />
            등록일: {new Date(template.created_at).toLocaleDateString('ko-KR')}
          </div>

          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={copyImageName}
              className="flex-1"
            >
              <Copy className="w-4 h-4 mr-1" />
              복사
            </Button>
            <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
              <DialogTrigger asChild>
                <Button size="sm" className="flex-1 bg-gradient-to-r from-purple-600 to-blue-600">
                  <Plus className="w-4 h-4 mr-1" />
                  생성
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>컨테이너 생성</DialogTitle>
                  <DialogDescription>
                    {template.image_name} 이미지로 새로운 컨테이너를 생성하시겠습니까?
                  </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setShowCreateDialog(false)}>
                    취소
                  </Button>
                  <Button
                    onClick={handleCreate}
                    disabled={isCreating}
                    className="bg-gradient-to-r from-purple-600 to-blue-600"
                  >
                    {isCreating && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                    생성
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

export function TemplatesPage() {
  const { templates, fetchTemplates, addTemplate, isLoading } = useTemplateStore();
  const { createContainer } = useContainerStore();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [imageName, setImageName] = useState('');

  useEffect(() => {
    fetchTemplates();
  }, [fetchTemplates]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!imageName.trim()) {
      toast.error('입력 오류', '이미지 이름은 필수입니다');
      return;
    }

    setIsSubmitting(true);
    try {
      await addTemplate(imageName.trim());
      toast.success('템플릿 추가됨', `${imageName} 템플릿이 성공적으로 추가되었습니다`);
      setImageName('');
      setIsDialogOpen(false);
    } catch (error: any) {
      toast.error('추가 실패', error.response?.data?.message || '템플릿 추가 중 오류가 발생했습니다');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateFromTemplate = async (imageName: string) => {
    await createContainer(imageName);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold gradient-text">템플릿 관리</h1>
          <p className="text-muted-foreground mt-1">
            Docker 이미지 템플릿을 관리하고 새 컨테이너를 생성하세요
          </p>
        </div>
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button className="bg-gradient-to-r from-purple-600 to-blue-600">
              <Plus className="w-4 h-4 mr-2" />
              템플릿 추가
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>새 템플릿 추가</DialogTitle>
              <DialogDescription>
                Docker 이미지 이름을 입력하여 새로운 템플릿을 등록하세요.
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="imageName">이미지 이름 *</Label>
                <Input
                  id="imageName"
                  placeholder="예: pytorch/pytorch:2.0.0-cuda11.7-cudnn8-runtime"
                  value={imageName}
                  onChange={(e) => setImageName(e.target.value)}
                  required
                />
                <p className="text-xs text-muted-foreground">
                  Docker Hub 이미지 또는 프라이빗 레지스트리 이미지 이름을 입력하세요
                </p>
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
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Card className="gradient-border">
          <CardHeader className="pb-2">
            <CardDescription>전체 템플릿</CardDescription>
            <CardTitle className="text-3xl">{templates.length}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center text-sm text-muted-foreground">
              <Layers className="w-4 h-4 mr-2" />
              사용 가능한 이미지
            </div>
          </CardContent>
        </Card>
        <Card className="gradient-border">
          <CardHeader className="pb-2">
            <CardDescription>인기 이미지</CardDescription>
            <CardTitle className="text-lg truncate">
              {templates.length > 0 ? templates[0].image_name.split('/')[0] : 'N/A'}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center text-sm text-muted-foreground">
              <Tag className="w-4 h-4 mr-2" />
              가장 많이 사용된 이미지
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Templates Grid */}
      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      ) : templates.length === 0 ? (
        <Card className="gradient-border">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <div className="w-16 h-16 rounded-full bg-secondary/50 flex items-center justify-center mb-4">
              <Package className="w-8 h-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-semibold mb-2">등록된 템플릿이 없습니다</h3>
            <p className="text-muted-foreground text-center max-w-md mb-4">
              Docker 이미지를 추가하여 컨테이너 생성에 사용할 템플릿을 구성하세요.
            </p>
            <Button
              onClick={() => setIsDialogOpen(true)}
              className="bg-gradient-to-r from-purple-600 to-blue-600"
            >
              <Plus className="w-4 h-4 mr-2" />
              첫 템플릿 추가
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {templates.map((template) => (
            <TemplateCard
              key={template.id}
              template={template}
              onCreate={handleCreateFromTemplate}
            />
          ))}
        </div>
      )}
    </div>
  );
}
