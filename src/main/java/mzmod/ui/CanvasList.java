package mzmod.ui;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public final class CanvasList extends Canvas implements Choice {
   private String[] items;
   private Image[] itemImages;
   private Font[] itemFonts;
   private boolean[] selectedFlags;
   private int topIndex;
   private int selectedIndex;
   private int itemCount;
   private int fitPolicy;
   private int choiceType;
   private int menuSelectedIndex;
   private int exclusiveSelectedIndex;
   private CommandListener commandListener;
   private Vector commands;
   private static Command SELECT_COMMAND;
   private Command selectCommand;
   private static Font DEFAULT_FONT;
   private int visibleItems;
   private boolean isMenuMode;
   private String title;
   private int itemHeight;
   private boolean isFullScreen;
   boolean isAnimating;
   private static final Calendar calendar;
   private static final Date date;
   private int scrollOffset;
   public static int bgColor;
   public static int textColor;
   public static int selectedGradientStart;
   public static int titleGradientStart;
   public static int titleGradientEnd;
   public static int menuTextColor;
   private Random random = new Random();
   boolean isScrolling;

   public CanvasList(String title, int choiceType) {
      super.setFullScreenMode(true);
      if (choiceType != 1 && choiceType != 2 && choiceType != 3) {
         throw new IllegalArgumentException();
      } else {
         this.choiceType = choiceType;
         this.itemHeight = DEFAULT_FONT.getHeight() + 4;
         this.title = title == null ? "" : title;
         this.setTitle(title);
         this.setFullScreenMode(true);
         this.selectCommand = SELECT_COMMAND;
         this.commands = new Vector();
         this.visibleItems = 1;
         this.selectedIndex = 1;
         this.isFullScreen = false;
         this.items = new String[20];
         this.itemFonts = new Font[20];
         this.itemImages = new Image[20];
         if (choiceType == 2) {
            this.selectedFlags = new boolean[20];
         }

         this.itemCount = 0;
      }
   }

   public final int append(String string, Image image) {
      if (string != null) {
         if (this.itemCount == this.items.length - 1) {
            this.expandArrays();
         }

         this.items[this.itemCount] = string;
         this.itemImages[this.itemCount] = image;
         this.itemFonts[this.itemCount] = DEFAULT_FONT;
         if (this.choiceType == 2) {
            this.selectedFlags[this.itemCount] = false;
         }

         this.repaint();
         return this.itemCount++;
      } else {
         return -1;
      }
   }

   public final void delete(int index) {
      if (index < this.itemCount && index >= 0) {
         System.arraycopy(this.items, index + 1, this.items, index, this.itemCount - index - 1);
         System.arraycopy(this.itemImages, index + 1, this.itemImages, index, this.itemCount - index - 1);
         System.arraycopy(this.itemFonts, index + 1, this.itemFonts, index, this.itemCount - index - 1);
         if (this.choiceType == 2) {
            System.arraycopy(this.selectedFlags, index + 1, this.selectedFlags, index, this.itemCount - index - 1);
         }
      }

      --this.itemCount;
      if (this.topIndex + this.selectedIndex > this.itemCount) {
         this.scrollUp();
      }

      this.repaint();
   }

   public final void deleteAll() {
      this.itemCount = 0;
      this.topIndex = 0;
      this.selectedIndex = 1;
   }

   public final int getFitPolicy() {
      return this.fitPolicy;
   }

   public final Font getFont(int index) {
      return this.itemFonts[index];
   }

   public final Image getImage(int index) {
      return this.itemImages[index];
   }

   public final int getSelectedFlags(boolean[] flags) {
      if (this.choiceType == 2) {
         int count = 0;

         for (int i = 0; i < this.itemCount; ++i) {
            if (flags[i] = this.selectedFlags[i]) {
               ++count;
            }
         }

         return count;
      } else {
         flags[this.topIndex + this.selectedIndex] = true;
         return 1;
      }
   }

   public final int getSelectedIndex() {
      if (this.choiceType == 3) {
         return this.topIndex + this.selectedIndex - 1;
      } else {
         return this.choiceType == 1 ? this.exclusiveSelectedIndex : -1;
      }
   }

   public final String getString(int index) {
      return index >= 0 && index < this.itemCount ? this.items[index] : null;
   }

   public final void insert(int index, String string, Image image) {
      if (index >= this.itemCount) {
         this.append(string, image);
      } else {
         if (index < 0) {
            index = 0;
         }

         if (this.itemCount == this.items.length - 1) {
            this.expandArrays();
         }

         for (int i = this.itemCount; i > index; --i) {
            String[] var10002 = this.items;
            var10002[i] = var10002[i - 1];
            Font[] var5 = this.itemFonts;
            var5[i] = var5[i - 1];
            Image[] var6 = this.itemImages;
            var6[i] = var6[i - 1];
            if (this.choiceType == 2) {
               boolean[] var7 = this.selectedFlags;
               var7[i] = var7[i - 1];
            }
         }

         ++this.itemCount;
         this.items[index] = string;
         this.itemFonts[index] = DEFAULT_FONT;
         this.itemImages[index] = image;
         if (this.choiceType == 2) {
            this.selectedFlags[index] = false;
         }
      }

      this.repaint();
   }

   public final boolean isSelected(int index) {
      if (this.choiceType == 2) {
         return this.selectedFlags[index];
      } else {
         return this.getSelectedIndex() == index;
      }
   }

   public final void removeCommand(Command command) {
      if (command == this.selectCommand) {
         this.selectCommand = null;
      }

      this.commands.removeElement(command);
   }

   public final void set(int index, String string, Image image) {
      if (index >= 0 && index < this.itemCount && string != null) {
         this.items[index] = string;
         this.itemImages[index] = image;
         this.repaint();
      }

   }

   public final void setFitPolicy(int policy) {
      if (policy != 0 && policy != 2 && policy != 1) {
         throw new IllegalArgumentException();
      } else {
         this.fitPolicy = policy;
      }
   }

   public final void setFont(int index, Font font) {
      if (index >= 0 && index < this.itemCount && font != null) {
         this.itemFonts[index] = font;
         this.repaint();
      }

   }

   public final void setSelectedFlags(boolean[] flags) {
      if (this.choiceType == 2) {
         System.arraycopy(flags, 0, this.selectedFlags, 0, this.itemCount);
      }

   }

   public final void setSelectedIndex(int index, boolean selected) {
      if (index < this.itemCount) {
         if (this.choiceType == 2 && selected) {
            this.selectedFlags[index] = selected;
         } else if (this.choiceType == 1) {
            this.exclusiveSelectedIndex = index;
         } else {
            this.selectedIndex = 1;
            this.topIndex = index;
         }
      }

      this.repaint();
   }

   public final int size() {
      return this.itemCount;
   }

   public final void addCommand(Command command) {
      if (command != null && !this.commands.contains(command)) {
         int cmdType = command.getCommandType();
         if (this.commands.size() <= 0) {
            this.commands.addElement(command);
         } else {
            int i = 0;

            while (true) {
               if (i >= this.commands.size()) {
                  this.commands.addElement(command);
                  break;
               }

               Command existing = (Command) this.commands.elementAt(i);
               if (cmdType == existing.getCommandType()) {
                  if (command.getPriority() > existing.getPriority()) {
                     this.commands.insertElementAt(command, i);
                     break;
                  }
               } else {
                  boolean insert;
                  label85: {
                     switch (existing.getCommandType()) {
                        case 1:
                           if (cmdType == 4 || cmdType == 8) {
                              insert = false;
                              break label85;
                           }
                           break;
                        case 2:
                           if (cmdType != 6 && cmdType != 7) {
                              insert = false;
                              break label85;
                           }

                           insert = true;
                           break label85;
                        case 3:
                           insert = cmdType == 6 || cmdType == 7 || cmdType == 2;
                           break label85;
                        case 4:
                        default:
                           break;
                        case 5:
                           if (cmdType == 1 || cmdType == 8 || cmdType == 4) {
                              insert = false;
                              break label85;
                           }
                           break;
                        case 6:
                           insert = false;
                           break label85;
                        case 7:
                           if (cmdType != 6) {
                              insert = false;
                              break label85;
                           }
                           break;
                        case 8:
                           if (cmdType == 4) {
                              insert = false;
                              break label85;
                           }
                     }

                     insert = true;
                  }

                  if (insert) {
                     this.commands.insertElementAt(command, i);
                     break;
                  }
               }

               ++i;
            }
         }

         this.repaint();
      }

   }

   public final void setCommandListener(CommandListener listener) {
      this.commandListener = listener;
   }

   private void expandArrays() {
      int newLen;
      String[] newItems = new String[newLen = (this.items.length << 1) + 10];
      System.arraycopy(this.items, 0, newItems, 0, this.itemCount);
      this.items = newItems;
      Font[] newFonts = new Font[newLen];
      System.arraycopy(this.itemFonts, 0, newFonts, 0, this.itemCount);
      this.itemFonts = newFonts;
      Image[] newImages = new Image[newLen];
      System.arraycopy(this.itemImages, 0, newImages, 0, this.itemCount);
      this.itemImages = newImages;
      if (this.choiceType == 2) {
         boolean[] newFlags = new boolean[newLen];
         System.arraycopy(this.selectedFlags, 0, newFlags, 0, this.itemCount);
         this.selectedFlags = newFlags;
      }

   }

   public final void keyRepeated(int keyCode) {
      this.keyPressed(keyCode);
   }

   private void startAnimation() {
      if (!this.isAnimating) {
         this.isAnimating = true;
         (new UiThread(this)).start();
      }

   }

   public final void keyPressed(int keyCode) {
      label174:
      while (true) {
         label169: {
            CanvasList var10000;
            boolean var10001;
            label168: {
               switch (keyCode) {
                  case -7:
                     if (!this.isFullScreen) {
                        if (this.isMenuMode) {
                           this.isMenuMode = false;
                        } else if (this.commandListener != null && this.commands.size() > 0) {
                           this.commandListener.commandAction((Command) this.commands.elementAt(0), this);
                        }
                        break label174;
                     }

                     var10000 = this;
                     break;
                  case -6:
                     if (!this.isFullScreen) {
                        if (this.isMenuMode) {
                           this.isMenuMode = false;
                           this.repaint();
                           if (this.commandListener != null) {
                              this.commandListener.commandAction((Command) this.commands.elementAt(this.menuSelectedIndex + 1), this);
                           }
                        } else if (this.commands.size() > 2) {
                           this.isMenuMode = true;
                        } else if (this.commandListener != null && this.commands.size() > 1) {
                           this.commandListener.commandAction((Command) this.commands.elementAt(1), this);
                        }
                        break label174;
                     }

                     var10000 = this;
                     break;
                  case -5:
                     if (!this.isMenuMode) {
                        if (this.choiceType == 3 && this.commandListener != null && this.selectCommand != null) {
                           this.commandListener.commandAction(this.selectCommand, this);
                           break label174;
                        }

                        keyCode = this.topIndex + this.selectedIndex - 1;
                        if (this.choiceType == 2) {
                           boolean[] var10002 = this.selectedFlags;
                           var10002[keyCode] = !var10002[keyCode];
                        } else if (this.choiceType == 1) {
                           if (this.exclusiveSelectedIndex == keyCode && this.commandListener != null && this.selectCommand != null) {
                              this.commandListener.commandAction(this.selectCommand, this);
                           } else {
                              this.exclusiveSelectedIndex = keyCode;
                           }
                        }
                        break label174;
                     }

                     keyCode = -6;
                     continue;
                  case -4:
                     if (!this.isMenuMode) {
                        if (this.itemCount <= this.visibleItems) {
                           this.topIndex = 0;
                           this.selectedIndex = this.selectedIndex == 1 ? this.itemCount : 1;
                        } else if (this.visibleItems + this.topIndex + this.selectedIndex <= this.itemCount) {
                           this.topIndex += this.visibleItems;
                        } else if (this.itemCount - this.topIndex <= this.visibleItems) {
                           this.topIndex = 0;
                           this.selectedIndex = 1;
                        } else {
                           this.topIndex += this.visibleItems;
                           this.selectedIndex = this.itemCount - this.topIndex;
                        }

                        this.startAnimation();
                     }
                     break label174;
                  case -3:
                     if (!this.isMenuMode) {
                        if (this.itemCount <= this.visibleItems) {
                           this.topIndex = 0;
                           this.selectedIndex = this.selectedIndex == 1 ? this.itemCount : 1;
                        } else if (this.topIndex >= this.visibleItems) {
                           this.topIndex -= this.visibleItems;
                        } else {
                           if (this.topIndex > 0) {
                              this.topIndex = 0;
                           } else if (this.itemCount > 0) {
                              this.topIndex = Math.max(0, this.itemCount - this.visibleItems);
                           }

                           this.selectedIndex = 1;
                        }

                        this.startAnimation();
                     }
                     break label174;
                  case -2:
                     if (!this.isMenuMode) {
                        if (this.selectedIndex < this.visibleItems) {
                           if (this.selectedIndex + this.topIndex < this.itemCount) {
                              ++this.selectedIndex;
                           } else {
                              this.topIndex = 0;
                              this.selectedIndex = 1;
                           }
                        } else if (this.selectedIndex + this.topIndex < this.itemCount) {
                           ++this.topIndex;
                        } else {
                           this.topIndex = 0;
                           this.selectedIndex = 1;
                        }

                        this.startAnimation();
                     } else {
                        --this.menuSelectedIndex;
                        if (this.menuSelectedIndex < 0) {
                           this.menuSelectedIndex = this.commands.size() - 2;
                        }
                     }
                     break label174;
                  case -1:
                     if (!this.isMenuMode) {
                        this.scrollUp();
                        this.startAnimation();
                     } else {
                        ++this.menuSelectedIndex;
                        if (this.menuSelectedIndex > this.commands.size() - 2) {
                           this.menuSelectedIndex = 0;
                        }
                     }
                  case 0:
                  case 1:
                  case 2:
                  case 3:
                  case 4:
                  case 5:
                  case 6:
                  case 7:
                  case 8:
                  case 9:
                  case 10:
                  case 11:
                  case 12:
                  case 13:
                  case 14:
                  case 15:
                  case 16:
                  case 17:
                  case 18:
                  case 19:
                  case 20:
                  case 21:
                  case 22:
                  case 23:
                  case 24:
                  case 25:
                  case 26:
                  case 27:
                  case 28:
                  case 29:
                  case 30:
                  case 31:
                  case 32:
                  case 33:
                  case 34:
                  case 36:
                  case 37:
                  case 38:
                  case 39:
                  case 40:
                  case 41:
                  case 43:
                  case 44:
                  case 45:
                  case 46:
                  case 47:
                  case 48:
                  default:
                     break label174;
                  case 35:
                     var10000 = this;
                     if (!this.isFullScreen) {
                        var10001 = true;
                        break label168;
                     }
                     break;
                  case 42:
                     break label169;
                  case 49:
                  case 50:
                  case 51:
                  case 52:
                  case 53:
                  case 54:
                  case 55:
                  case 56:
                  case 57:
                     keyCode -= 48;
                     if (!this.isMenuMode && keyCode + this.topIndex <= this.itemCount) {
                        this.selectedIndex = keyCode;
                        this.repaint();
                        if (this.choiceType == 3 && this.commandListener != null && this.selectCommand != null) {
                           this.commandListener.commandAction(this.selectCommand, this);
                        }
                     }
                     break label169;
               }

               var10001 = false;
            }

            var10000.isFullScreen = var10001;
            break;
         }

         bgColor = this.random.nextInt(16777215);
         titleGradientEnd = this.random.nextInt(16777215);
         titleGradientStart = this.random.nextInt(16777215);
         textColor = this.random.nextInt(16777215);
         selectedGradientStart = this.random.nextInt(16777215);
         menuTextColor = this.random.nextInt(16777215);
         break;
      }

      this.repaint();
   }

   private void scrollUp() {
      if (this.selectedIndex > 1) {
         --this.selectedIndex;
      } else if (this.topIndex > 0) {
         --this.topIndex;
      } else {
         if (this.itemCount > 0) {
            this.topIndex = Math.max(0, this.itemCount - this.visibleItems);
            this.selectedIndex = this.itemCount - this.topIndex;
         }

      }
   }

   public static void resetColors() {
      bgColor = 0;
      textColor = 16777215;
      selectedGradientStart = 16711680;
      titleGradientStart = 65280;
      titleGradientEnd = 65280;
      menuTextColor = 16776960;
   }

   protected final void paint(Graphics g) {
      int width = super.getWidth();
      int height = super.getHeight();
      g.setColor(bgColor);
      g.fillRect(0, 0, width, height);
      g.setColor(textColor);
      g.setFont(DEFAULT_FONT);
      int y = (this.isFullScreen ? 0 : this.itemHeight) + 1;
      int bottomY = this.isFullScreen ? height - 4 : height - (DEFAULT_FONT.getHeight() << 1) + 4;
      int i;
      int itemW;
      int var10;
      int var12;
      if (this.itemCount > 0) {
         i = this.topIndex;

         for (this.visibleItems = 0; i < this.itemCount && y < bottomY; ++i) {
            ++this.visibleItems;
            Font font = this.itemFonts[i];
            Image image = this.itemImages[i];
            var10 = font.getHeight();
            itemW = 4 + (image != null && image.getHeight() > var10 ? image.getHeight() : var10);
            g.setFont(font);
            String text = this.items[i];
            if (i == this.topIndex + this.selectedIndex - 1) {
               var12 = 0;
               int gradStep = this.computeGradientStep(selectedGradientStart);

               for (int lineY = y + itemW; lineY > y; --lineY) {
                  g.setColor(var12 -= gradStep);
                  g.drawLine(0, lineY, width, lineY);
               }

               g.setColor(textColor);
               if (15 + (image == null ? 0 : image.getWidth()) + DEFAULT_FONT.stringWidth(text) > width) {
                  if (!this.isScrolling) {
                     this.scrollOffset = 0;
                     this.isScrolling = true;
                     (new ScrollThread(this)).start();
                  } else if (this.scrollOffset < text.length() && 20 + (image == null ? 0 : image.getWidth()) + DEFAULT_FONT.stringWidth(text.substring(this.scrollOffset)) > width) {
                     text = text.substring(++this.scrollOffset);
                  } else {
                     this.scrollOffset = 0;
                  }
               } else {
                  this.isScrolling = false;
               }
            }

            var12 = y + (itemW - font.getHeight()) / 2;
            if (this.choiceType == 3) {
               if (image != null) {
                  g.drawImage(image, 2, y + 2, 20);
               }

               g.drawString(text, 3 + (image == null ? 0 : image.getWidth()), var12, 20);
            } else if (this.choiceType == 1) {
               g.drawArc(2, var12, var10 - 2, var10 - 2, 0, 360);
               if (this.exclusiveSelectedIndex == i) {
                  g.fillArc(7, var12 + 5, var10 - 10, var10 - 10, 0, 360);
               }

               if (image != null) {
                  g.drawImage(image, var10 + 2, y + 2, 20);
               }

               g.drawString(text, var10 + 5 + (image == null ? 0 : image.getWidth()), y + (itemW - font.getHeight()) / 2, 20);
            } else {
               g.drawRect(2, var12, var10, var10);
               if (this.selectedFlags[i]) {
                  g.setColor(65280);
                  g.drawLine(3, var12 + var10 / 2, var10 / 2 + 1, var12 + var10 - 1);
                  g.drawLine(var10 + 2, var12 + 2, var10 / 2 + 1, var12 + var10 - 1);
               }

               if (image != null) {
                  g.drawImage(image, itemW + 2, y + 2, 20);
               }

               g.setColor(textColor);
               g.drawString(text, itemW + 5 + (image == null ? 0 : image.getWidth()), y + (itemW - font.getHeight()) / 2, 20);
            }

            y += itemW;
         }
      }

      i = height - this.itemHeight;
      int var16;
      int var17;
      if (!this.isFullScreen) {
         itemW = 0;
         var16 = this.computeGradientStep(titleGradientStart);

         for (var17 = this.itemHeight; var17 >= 0; --var17) {
            g.setColor(itemW += var16);
            g.drawLine(0, var17, width, var17);
         }

         itemW = 0;
         var16 = this.computeGradientStep(titleGradientEnd);

         for (var17 = i; var17 <= height; ++var17) {
            g.setColor(itemW += var16);
            g.drawLine(0, var17, width, var17);
         }

         i += 2;
         g.setColor(menuTextColor);
         g.drawString(this.title, width >> 1, 2, 17);
         String statusText;
         if (this.isAnimating) {
            statusText = this.getSelectedIndex() + 1 + "/" + this.itemCount;
         } else {
            date.setTime(System.currentTimeMillis());
            calendar.setTime(date);
            StringBuffer timeBuf = new StringBuffer();
            if ((bottomY = calendar.get(11)) < 10) {
               timeBuf.append('0');
            }

            timeBuf.append(bottomY).append(':');
            if ((bottomY = calendar.get(12)) < 10) {
               timeBuf.append('0');
            }

            statusText = timeBuf.append(bottomY).toString();
         }

         g.drawString(statusText, width >> 1, i, 17);
      }

      if ((itemW = this.commands.size()) > 0) {
         if (!this.isMenuMode) {
            if (!this.isFullScreen) {
               if (itemW > 1) {
                  g.drawString(itemW > 2 ? "Options" : ((Command) this.commands.elementAt(1)).getLabel(), 2, i, 20);
               }

               g.drawString(((Command) this.commands.elementAt(0)).getLabel(), width - 2, i, 24);
               return;
            }
         } else {
            if (itemW > 1) {
               g.drawString("Select", 2, i, 20);
               g.drawString("Cancel", width - 2, i, 24);
            }

            var16 = 0;

            for (var17 = 1; var17 < this.commands.size(); ++var17) {
               if (DEFAULT_FONT.stringWidth(((Command) this.commands.elementAt(var17)).getLabel()) > var16) {
                  var16 = DEFAULT_FONT.stringWidth(((Command) this.commands.elementAt(var17)).getLabel());
               }
            }

            var16 += 4;
            var17 = this.commands.size() - 1;
            var10 = (this.itemHeight - 2) * var17 + 3;
            int menuTop = height - this.itemHeight - var10 - 1;
            g.setColor(16777215);
            g.drawRect(0, menuTop, var16, var10);
            g.setColor(16514047);
            g.fillRect(1, menuTop + 1, var16 - 1, var10 - 1);
            g.setColor(0);
            menuTop += var10;

            for (var12 = 1; var12 <= var17; ++var12) {
               g.drawString(((Command) this.commands.elementAt(var12)).getLabel(), 2, menuTop - (this.itemHeight - 2) * var12, 20);
            }

            g.setColor(0);
            g.fillRect(1, menuTop - 1 - (this.itemHeight - 2) * (this.menuSelectedIndex + 1), var16 - 1, this.itemHeight - 1);
            g.setColor(16514047);
            g.drawString(((Command) this.commands.elementAt(this.menuSelectedIndex + 1)).getLabel(), 2, menuTop - (this.itemHeight - 2) * (this.menuSelectedIndex + 1), 20);
         }
      }

   }

   private int computeGradientStep(int color) {
      int blue = (color & 255) / this.itemHeight;
      int green = (color >> 8 & 255) / this.itemHeight;
      color = (color >> 16) / this.itemHeight;
      return blue | green << 8 | color << 16;
   }

   public final void setTitle(String title) {
      if (title != null) {
         this.title = title;
      }

   }

   static {
      SELECT_COMMAND = List.SELECT_COMMAND;
      DEFAULT_FONT = Font.getFont(0, 0, 8);
      calendar = Calendar.getInstance();
      date = new Date();
   }
}
