package mzmod.ui;

public final class UiThread extends Thread {
   private CanvasList canvasList;

   public UiThread(CanvasList canvasList) {
      this.canvasList = canvasList;
   }

   public final void run() {
      try {
         Thread.sleep(1000L);
      } catch (Exception var1) {
      }

      this.canvasList.isAnimating = false;
      this.canvasList.repaint();
   }
}
