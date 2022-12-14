package com.example.tetris;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.example.juiexample.common.dialog.CustomDialog;
import com.example.juiexample.event.EventHandler;
import com.example.juiexample.event.OnEventListener;
import com.example.juiexample.utils.ToastUtils;
import com.example.tetris.model.GameDef;
import com.example.tetris.settings.SettingsDialog;
import com.example.tetris.model.TetrisGame;
import com.example.tetris.view.GameLayout;

public class TetrisDialog extends CustomDialog implements OnEventListener {

    private final Context context;
    private final TetrisGame game;
    private final GameLayout tetrisLayout;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final SettingsDialog settingsDialog;

    public TetrisDialog(Context context) {
        super(context);
        this.context = context;
        settingsDialog = new SettingsDialog(context);
        tetrisLayout = new GameLayout(context);
        tetrisLayout.setOnEventListener(this::onEvent);
        EventHandler.getInstance()
                .addOnEventListener(GameDef.GAME_EVENT_OVER, this)
                .addOnEventListener(GameDef.GAME_EVENT_KEY_UP, this)
                .addOnEventListener(GameDef.GAME_EVENT_SCORE_UPDATE, this)
                .addOnEventListener(GameDef.GAME_EVENT_SPEED_UPDATE, this)
                .addOnEventListener(GameDef.GAME_EVENT_KEY_START, this)
                .addOnEventListener(GameDef.GAME_EVENT_KEY_STOP, this);
        setView(tetrisLayout);
        setTitle("俄罗斯方块");
        setRightTitle("设置");
        setRightListener(v -> settingsDialog.show());
        game = new TetrisGame(context);
        game.setInterval(600);
        game.setActiveBoxTask(new Runnable() {
            @Override
            public void run() {
                uiHandler.post(() -> tetrisLayout.moveActiveBox());
            }
        });

        tetrisLayout.setScore(game.getScore());
    }

    @Override
    public void onEvent(String key, Bundle bundle) {
        if (GameDef.GAME_EVENT_KEY_START.equals(key)) {
            game.start();
        } else if (GameDef.GAME_EVENT_OVER.equals(key)) {
            ToastUtils.showLong(context, "Game Over!!!");
            game.stop();
        } else if (GameDef.GAME_EVENT_KEY_STOP.equals(key)) {
            game.stop();
        } else if (GameDef.GAME_EVENT_KEY_UP.equals(key)) {
            tetrisLayout.switchActiveBoxStyle();
        } else if (GameDef.GAME_EVENT_SCORE_UPDATE.equals(key)) {
            int score = game.getScore();
            score++;
            game.setScore(score);
            tetrisLayout.setScore(score);
        } else if (GameDef.GAME_EVENT_SPEED_UPDATE.equals(key)) {
            int interval = bundle.getInt("refresh_interval");
            game.setInterval(interval);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        game.stop();
        EventHandler.getInstance().removeAll();
    }
}
