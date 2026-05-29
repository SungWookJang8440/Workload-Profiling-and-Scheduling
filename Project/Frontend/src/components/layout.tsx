import { useEffect, useState } from 'react';
import { Outlet, useLocation, Link, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard,
  Box,
  Server,
  Layers,
  User,
  LogOut,
  Menu,
  X,
  ChevronRight,
  Settings,
  Cpu,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Separator } from '@/components/ui/separator';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { useAuthStore } from '@/lib/store';
import { cn } from '@/lib/utils';

const sidebarItems = [
  { icon: LayoutDashboard, label: '대시보드', href: '/dashboard' },
  { icon: Box, label: '내 컨테이너', href: '/containers' },
  { icon: Server, label: '클러스터', href: '/clusters' },
  { icon: Layers, label: '템플릿', href: '/templates' },
];

function SidebarItem({ icon: Icon, label, href, isActive, collapsed }: {
  icon: typeof LayoutDashboard;
  label: string;
  href: string;
  isActive: boolean;
  collapsed: boolean;
}) {
  return (
    <TooltipProvider delayDuration={0}>
      <Tooltip>
        <TooltipTrigger asChild>
          <Link
            to={href}
            className={cn(
              'flex items-center gap-3 px-3 py-2 rounded-lg transition-all duration-200 group',
              isActive
                ? 'bg-primary/10 text-primary'
                : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
            )}
          >
            <Icon className={cn('w-5 h-5', isActive && 'text-primary')} />
            {!collapsed && <span className="font-medium">{label}</span>}
            {isActive && !collapsed && (
              <motion.div
                layoutId="activeIndicator"
                className="ml-auto w-1.5 h-1.5 rounded-full bg-primary"
              />
            )}
          </Link>
        </TooltipTrigger>
        {collapsed && (
          <TooltipContent side="right">
            <p>{label}</p>
          </TooltipContent>
        )}
      </Tooltip>
    </TooltipProvider>
  );
}

function UserMenu() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-3 w-full p-2 rounded-lg hover:bg-accent transition-colors"
      >
        <Avatar className="w-8 h-8 border-2 border-primary/20">
          <AvatarImage src={`https://avatar.vercel.sh/${user?.email}`} />
          <AvatarFallback className="bg-primary/10 text-primary text-sm">
            {user?.username?.[0]?.toUpperCase()}
          </AvatarFallback>
        </Avatar>
        <div className="flex-1 text-left">
          <p className="text-sm font-medium truncate">{user?.username}</p>
          <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
        </div>
        <ChevronRight className={cn('w-4 h-4 transition-transform', isOpen && 'rotate-90')} />
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            className="absolute bottom-full left-0 right-0 mb-2 bg-popover border rounded-lg shadow-lg p-1"
          >
            <Link
              to="/profile"
              className="flex items-center gap-2 px-3 py-2 text-sm rounded-md hover:bg-accent"
            >
              <User className="w-4 h-4" />
              프로필
            </Link>
            <Link
              to="/settings"
              className="flex items-center gap-2 px-3 py-2 text-sm rounded-md hover:bg-accent"
            >
              <Settings className="w-4 h-4" />
              설정
            </Link>
            <Separator className="my-1" />
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 px-3 py-2 text-sm rounded-md hover:bg-destructive/10 hover:text-destructive w-full text-left"
            >
              <LogOut className="w-4 h-4" />
              로그아웃
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export function Layout() {
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  return (
    <div className="min-h-screen bg-background">
      {/* Mobile Header */}
      <header className="lg:hidden fixed top-0 left-0 right-0 h-16 bg-background/95 backdrop-blur border-b z-50 px-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-purple-500 to-blue-500 flex items-center justify-center">
            <Cpu className="w-4 h-4 text-white" />
          </div>
          <span className="font-bold gradient-text">GPU Sharing</span>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setMobileOpen(!mobileOpen)}
        >
          {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </Button>
      </header>

      {/* Mobile Sidebar Overlay */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="lg:hidden fixed inset-0 bg-black/50 z-40"
            onClick={() => setMobileOpen(false)}
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed left-0 top-0 bottom-0 bg-card border-r z-40 transition-all duration-300',
          collapsed ? 'w-16' : 'w-64',
          mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        )}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="h-16 flex items-center px-4 border-b">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-purple-500 to-blue-500 flex items-center justify-center mr-3">
              <Cpu className="w-4 h-4 text-white" />
            </div>
            {!collapsed && (
              <span className="font-bold text-lg gradient-text">GPU Sharing</span>
            )}
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-3 space-y-1">
            {sidebarItems.map((item) => (
              <SidebarItem
                key={item.href}
                {...item}
                isActive={location.pathname === item.href}
                collapsed={collapsed}
              />
            ))}
          </nav>

          {/* Bottom Section */}
          <div className="p-3 border-t space-y-3">
            {/* Collapse Button (Desktop only) */}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setCollapsed(!collapsed)}
              className="hidden lg:flex w-full justify-center"
            >
              <Menu className={cn('w-4 h-4 transition-transform', collapsed && 'rotate-180')} />
            </Button>

            {!collapsed && <UserMenu />}
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main
        className={cn(
          'min-h-screen transition-all duration-300 pt-16 lg:pt-0',
          collapsed ? 'lg:pl-16' : 'lg:pl-64'
        )}
      >
        <div className="p-6 lg:p-8 max-w-7xl mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
