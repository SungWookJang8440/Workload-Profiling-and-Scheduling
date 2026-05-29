import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Cpu, Loader2, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuthStore } from '@/lib/store';
import { toast } from '@/lib/toast';

export function LoginPage() {
  const navigate = useNavigate();
  const { login, isLoading } = useAuthStore();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(email, password);
      toast.success('로그인 성공', '환영합니다!');
      navigate('/dashboard');
    } catch (error: any) {
      toast.error('로그인 실패', error.response?.data?.message || '이메일 또는 비밀번호를 확인하세요');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-background to-primary/5 p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-purple-500 to-blue-500 mb-4">
            <Cpu className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold gradient-text mb-2">GPU Sharing</h1>
          <p className="text-muted-foreground">AI/ML 개발환경을 빠르게 구성하세요</p>
        </div>

        <div className="mb-4">
          <p className="text-xs text-red-400 mb-1">테스트: 아래 입력이 되면 React 문제, 안 되면 CSS 문제</p>
          <input 
            type="text" 
            placeholder="원시 HTML input 테스트"
            className="w-full p-2 border border-red-500 rounded bg-white text-black"
            onChange={(e) => console.log('raw input:', e.target.value)}
          />
        </div>

        <Card className="border-0 shadow-2xl">
          <CardHeader className="space-y-1">
            <CardTitle className="text-2xl">로그인</CardTitle>
            <CardDescription>
              계정에 로그인하여 GPU 리소스에 접근하세요
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">이메일</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="bg-secondary/50"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">비밀번호</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className="bg-secondary/50"
                />
              </div>
              <Button
                type="submit"
                className="w-full bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700"
                disabled={isLoading}
              >
                {isLoading ? (
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                ) : (
                  <ArrowRight className="w-4 h-4 mr-2" />
                )}
                로그인
              </Button>
            </form>

            <div className="mt-6 text-center text-sm">
              <span className="text-muted-foreground">계정이 없으신가요? </span>
              <Link to="/register" className="text-primary hover:underline font-medium">
                회원가입
              </Link>
            </div>
          </CardContent>
        </Card>

        <div className="mt-8 flex items-center justify-center gap-2 text-sm text-muted-foreground">
          <span>Spring Boot + React + GPU Cluster</span>
        </div>
      </div>
    </div>
  );
}
