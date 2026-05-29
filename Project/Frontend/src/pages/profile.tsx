import { useState } from 'react';
import { motion } from 'framer-motion';
import { User, Mail, Key, Shield, Save, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { useAuthStore } from '@/lib/store';
import { toast } from '@/lib/toast';
import api from '@/lib/api';

export function ProfilePage() {
  const { user } = useAuthStore();
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      toast.error('비밀번호 불일치', '새 비밀번호가 일치하지 않습니다');
      return;
    }

    setIsChangingPassword(true);
    try {
      await api.changePassword(passwordForm.currentPassword, passwordForm.newPassword);
      toast.success('비밀번호 변경됨', '비밀번호가 성공적으로 변경되었습니다');
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (error: any) {
      toast.error('변경 실패', error.response?.data?.message || '비밀번호 변경 중 오류가 발생했습니다');
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold gradient-text">프로필</h1>
        <p className="text-muted-foreground mt-1">
          계정 정보를 확인하고 설정을 관리하세요
        </p>
      </div>

      {/* Profile Info */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3 }}
        >
          <Card className="gradient-border h-full">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <User className="w-5 h-5 text-primary" />
                계정 정보
              </CardTitle>
              <CardDescription>
                사용자 기본 정보입니다
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center gap-4">
                <Avatar className="w-20 h-20 border-4 border-primary/20">
                  <AvatarImage src={`https://avatar.vercel.sh/${user?.email}`} />
                  <AvatarFallback className="bg-primary/10 text-primary text-2xl">
                    {user?.username?.[0]?.toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <h3 className="text-xl font-semibold">{user?.username}</h3>
                  <div className="flex items-center gap-2 mt-1">
                    <Mail className="w-4 h-4 text-muted-foreground" />
                    <span className="text-sm text-muted-foreground">{user?.email}</span>
                  </div>
                  <div className="flex gap-2 mt-2">
                    <Badge variant={user?.is_admin ? 'destructive' : 'default'}>
                      {user?.is_admin ? (
                        <span className="flex items-center gap-1">
                          <Shield className="w-3 h-3" />
                          관리자
                        </span>
                      ) : (
                        '일반 사용자'
                      )}
                    </Badge>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <Card className="gradient-border h-full">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Key className="w-5 h-5 text-primary" />
                비밀번호 변경
              </CardTitle>
              <CardDescription>
                계정 보안을 위해 주기적으로 비밀번호를 변경하세요
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleChangePassword} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="currentPassword">현재 비밀번호</Label>
                  <Input
                    id="currentPassword"
                    type="password"
                    placeholder="현재 비밀번호 입력"
                    value={passwordForm.currentPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="newPassword">새 비밀번호</Label>
                  <Input
                    id="newPassword"
                    type="password"
                    placeholder="새 비밀번호 입력"
                    value={passwordForm.newPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">새 비밀번호 확인</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    placeholder="새 비밀번호 다시 입력"
                    value={passwordForm.confirmPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                    required
                  />
                </div>
                <Button
                  type="submit"
                  disabled={isChangingPassword}
                  className="w-full bg-gradient-to-r from-purple-600 to-blue-600"
                >
                  {isChangingPassword ? (
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  ) : (
                    <Save className="w-4 h-4 mr-2" />
                  )}
                  비밀번호 변경
                </Button>
              </form>
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}
