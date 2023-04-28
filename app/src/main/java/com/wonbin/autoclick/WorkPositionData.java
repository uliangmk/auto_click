package com.wonbin.autoclick;

/**
 * @Author： ZhangYuLiang
 * @description：
 */
public class WorkPositionData {
    public int workX;//点击x
    public int workY;//点击y
    public int toX;//滑动到x
    public int toY;//滑动到y
    public String mode = AutoService.MODE_CLICK;//点击还是滑动  CLICK SWIPE
    public long interval;//延时毫秒

    public WorkPositionData(int x, int y) {
        workX = x;
        workY = y;
    }

    public WorkPositionData() {
    }
}
