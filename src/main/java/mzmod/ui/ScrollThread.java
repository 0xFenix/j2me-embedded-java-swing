package mzmod.ui;

public final class ScrollThread extends Thread {
   private CanvasList canvasList;

   public ScrollThread(CanvasList canvasList) {
      this.canvasList = canvasList;
   }

   public final void run() {
      while (this.canvasList.isScrolling) {
         try {
            Thread.sleep(500L);
            this.canvasList.repaint();
         } catch (Exception var1) {
         }
      }
   }
}
