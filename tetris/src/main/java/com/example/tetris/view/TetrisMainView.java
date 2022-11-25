package com.example.tetris.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Size;

import com.example.common.TLog;
import com.example.juiexample.event.EventHandler;
import com.example.juiexample.event.OnEventListener;
import com.example.tetris.GameDef;
import com.example.tetris.GridType;
import com.example.tetris.GridTypeMatrix;
import com.example.tetris.GridTypeMatrixManager;

public class TetrisMainView extends TetrisGrid implements OnEventListener {

    private static final String TAG = "TetrisMainView";
    private final ActiveBox activeBox;
    private final Rect rect = new Rect();
    private boolean gameOver = false;
    private GridType defaultGridType;

    public TetrisMainView(Context context, Size size) {
        super(context, size);
        activeBox = new ActiveBox(context, size);
        EventHandler.getInstance()
                .addOnEventListener(GameDef.GAME_EVENT_KEY_LEFT, this)
                .addOnEventListener(GameDef.GAME_EVENT_KEY_RIGHT, this)
                .addOnEventListener(GameDef.GAME_EVENT_KEY_DOWN, this);
    }

    @Override
    public TetrisMainView setGridSize(Size size) {
        super.setGridSize(size);
        return this;
    }

    @Override
    public TetrisMainView setGridType(GridTypeMatrix gridTypes) {
        super.setGridType(gridTypes);
        defaultGridType = gridTypes.getGridType(0, 0).clone();
        rect.left = 0;
        rect.top = 0;
        rect.right = gridCount.getWidth();
        rect.bottom = gridCount.getHeight();

        updateActiveBoxGridType(GridTypeMatrixManager.getInstance().getRandomMatrix());
        addView(activeBox);
        return this;
    }

    private TetrisMainView updateActiveBoxGridType(GridTypeMatrix gridType) {
        gridType.randomColor();
        activeBox.setGridType(gridType);
        Rect activeBoxRect = new Rect();
        activeBoxRect.left = (gridCount.getWidth() - activeBox.gridCount.getWidth()) / 2;
        activeBoxRect.right = activeBoxRect.left + activeBox.gridCount.getWidth();
        activeBoxRect.top = 0;
        activeBoxRect.bottom = activeBoxRect.top + activeBox.gridCount.getHeight();
        activeBox.setRect(activeBoxRect);
        return this;
    }

    public TetrisMainView moveActiveBox(GameDef.KeyType keyType) {
        if (gameOver) {
            return this;
        }
        Rect activeBoxRect = new Rect(activeBox.getRect());
        TLog.d(TAG, rect.toString());
        TLog.d(TAG, activeBoxRect.toString());
        if (keyType == GameDef.KeyType.TYPE_MOVE_DOWN) {
            activeBoxRect.top += 1;
            activeBoxRect.bottom += 1;
            if (checkRect(rect, activeBoxRect, activeBox.gridTypes)){
                activeBox.moveDown();
            } else {
                update();
                gameOver = checkGameOver();
                if (gameOver) {
                    EventHandler.getInstance().sendEvent(GameDef.GAME_EVENT_OVER, null);
                    return this;
                }
            }
        } else if (keyType == GameDef.KeyType.TYPE_MOVE_LEFT) {
            activeBoxRect.left -= 1;
            activeBoxRect.right -= 1;
            if (checkRect(rect, activeBoxRect, activeBox.gridTypes)){
                activeBox.moveLeft();
            }
        } else if (keyType == GameDef.KeyType.TYPE_MOVE_RIGHT) {
            activeBoxRect.left += 1;
            activeBoxRect.right += 1;
            if (checkRect(rect, activeBoxRect, activeBox.gridTypes)){
                activeBox.moveRight();
            }
        } else if (keyType == GameDef.KeyType.TYPE_SWITCH_STYLE) {
            switchStyle();
        }
        return this;
    }

    private void switchStyle() {
        Rect activeBoxRect = new Rect(activeBox.getRect());
        int w = activeBox.gridCount.getWidth();
        int h = activeBox.gridCount.getHeight();
        if (activeBoxRect.top < w - h) {
            TLog.w(TAG, "垂直空间不足，无法翻转");
            return;
        }
        GridTypeMatrix gridType = activeBox.gridTypes.rotate90();
        activeBoxRect.top = activeBoxRect.bottom - w;
        activeBoxRect.right = activeBoxRect.left + h;
        if (checkRect(rect, activeBoxRect, gridType)) {
            activeBox.setGridType(gridType);
            activeBox.setRect(activeBoxRect);
        }
    }

    private void update() {
        TLog.d(TAG, "update");
        Rect activeBoxRect = activeBox.getRect();
        for (int i = activeBoxRect.left; i < activeBoxRect.right; i++) {
            for (int j = activeBoxRect.top; j < activeBoxRect.bottom; j++) {
                GridView view = activeBox.mainViews[j - activeBoxRect.top][i - activeBoxRect.left];
                GridType oldType = mainViews[j][i].getGridType();
                TLog.d(TAG, mainViews[j][i].toString());
                if (oldType.isNone && !view.getGridType().isNone) {
                    gridTypes.getGridType(j, i).clone(view.getGridType());
                    mainViews[j][i].setGridType(gridTypes.getGridType(j, i));
                }
            }
        }
        for (int j = gridCount.getHeight() - 1; j >= 0; j--) {
            int clearLineIndex = j;
            for (int i = 0; i < gridCount.getWidth(); i++) {
                if (mainViews[j][i].getGridType().isNone) {
                    clearLineIndex = -1;
                    break;
                }
            }
            if (clearLineIndex == j) {
                clearLine(clearLineIndex);
                j++;
            }
        }
        updateActiveBoxGridType(GridTypeMatrixManager.getInstance().getRandomMatrix());
    }

    private void clearLine(int lineIndex) {
        TLog.d(TAG, "clearLine:"+lineIndex);
        EventHandler.getInstance().sendEvent(GameDef.GAME_EVENT_SCORE_UPDATE, null);
        for (int j = lineIndex; j >= 0; j--) {
            for (int i = 0; i < gridCount.getWidth(); i++) {
                if (j == 0) {
                    gridTypes.getGridType(j, i).clone(defaultGridType);
                    mainViews[j][i].setGridType(gridTypes.getGridType(j, i));
                } else {
                    gridTypes.getGridType(j, i).clone(gridTypes.getGridType(j - 1, i));
                    mainViews[j][i].setGridType(gridTypes.getGridType(j, i));
                }
            }
        }
    }

    private boolean checkGameOver() {
        Rect activeBoxRect = activeBox.getRect();
        TLog.d(TAG, "checkGameOver, rect=" + rect);
        TLog.d(TAG, "checkGameOver, activeBoxRect=" + activeBoxRect);
        for (int i = activeBoxRect.left; i < activeBoxRect.right; i++) {
            for (int j = activeBoxRect.top; j < activeBoxRect.bottom; j++) {
                GridView boxGrid = activeBox.mainViews[j - activeBoxRect.top][i - activeBoxRect.left];
                GridView mainGrid = mainViews[j][i];
                if (!boxGrid.getGridType().isNone && !mainGrid.getGridType().isNone) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkRect(Rect outRect, Rect inRect, GridTypeMatrix inMatrix) {
        // 边界检查
        if (inRect.left < outRect.left
                || inRect.top < outRect.top
                || inRect.right > outRect.right
                || inRect.bottom > outRect.bottom) {
            return false;
        }

        // 重叠检查
        for (int i = inRect.top; i < inRect.bottom; i++) {
            for (int j = inRect.left; j < inRect.right; j++) {
                GridType inGridType = inMatrix.getGridType(i - inRect.top, j - inRect.left);
                GridType outGridType = gridTypes.getGridType(i, j);
                TLog.d(TAG, String.format("i=%s, j=%s, inGridType=%s, outGridType=%s", i, j, inGridType.isNone, outGridType.isNone));
                if (!inGridType.isNone && !outGridType.isNone) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onEvent(String key, Bundle bundle) {
        if (GameDef.GAME_EVENT_KEY_DOWN.equals(key)) {
            moveActiveBox(GameDef.KeyType.TYPE_MOVE_DOWN);
        } else if (GameDef.GAME_EVENT_KEY_LEFT.equals(key)) {
            moveActiveBox(GameDef.KeyType.TYPE_MOVE_LEFT);
        } else if (GameDef.GAME_EVENT_KEY_RIGHT.equals(key)) {
            moveActiveBox(GameDef.KeyType.TYPE_MOVE_RIGHT);
        }
    }
}
