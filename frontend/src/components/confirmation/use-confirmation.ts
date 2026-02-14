import type { ElMessageBoxOptions } from 'element-plus';
import { ElMessageBox } from 'element-plus';

export function useConfirmation(message: string, options: ElMessageBoxOptions, onConfirm: () => Promise<unknown>) {
  return async () => {
    try {
      await ElMessageBox.confirm(message, options);
    } catch (_) {
      return;
    }
    await onConfirm();
  };
}
