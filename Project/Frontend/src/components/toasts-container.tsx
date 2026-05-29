import { useToasts } from '@/lib/toast';
import { motion, AnimatePresence } from 'framer-motion';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { toast as toastLib } from '@/lib/toast';
import { cn } from '@/lib/utils';

const icons = {
  default: Info,
  success: CheckCircle,
  destructive: AlertCircle,
};

const colors = {
  default: 'bg-blue-500/10 border-blue-500/20 text-blue-500',
  success: 'bg-green-500/10 border-green-500/20 text-green-500',
  destructive: 'bg-red-500/10 border-red-500/20 text-red-500',
};

export function ToastsContainer() {
  const toasts = useToasts();

  return (
    <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none">
      <AnimatePresence>
        {toasts.map((toast) => {
          const Icon = icons[toast.variant || 'default'];
          return (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: 20, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, x: 100 }}
              className={cn(
                'pointer-events-auto w-80 p-4 rounded-lg border shadow-lg backdrop-blur-sm',
                colors[toast.variant || 'default']
              )}
            >
              <div className="flex items-start gap-3">
                <Icon className="w-5 h-5 mt-0.5 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-sm">{toast.title}</p>
                  {toast.description && (
                    <p className="text-xs opacity-80 mt-1">{toast.description}</p>
                  )}
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  className="shrink-0 h-6 w-6 -mr-1 -mt-1"
                  onClick={() => toastLib.remove(toast.id)}
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}
