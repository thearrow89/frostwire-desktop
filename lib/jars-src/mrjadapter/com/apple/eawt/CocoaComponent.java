/*   */ package com.apple.eawt;
/*   */ 
/*   */ import java.awt.Canvas;
/*   */ import java.awt.Dimension;
/*   */ 
/*   */ public abstract class CocoaComponent extends Canvas
/*   */ {
/*   */   public abstract int createNSView();
/*   */ 
/*   */   public long createNSViewLong()
/*   */   {
/* 7 */     return 0L;
/*   */   }
/*   */ 
/*   */   public abstract Dimension getMaximumSize();
/*   */ 
/*   */   public abstract Dimension getMinimumSize();
/*   */ 
/*   */   public abstract Dimension getPreferredSize();
/*   */ 
/*   */   public final void sendMessage(int paramInt, Object paramObject)
/*   */   {
/*   */   }
/*   */ }
