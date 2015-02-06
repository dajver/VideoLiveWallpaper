package com.video.live.wallpaper;

import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.io.InputStream;

public class VideoLiveWallpaperService extends WallpaperService {

    static final String TAG = "NYAN";
    static final Handler mNyanHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    //создаем вью которое будет отображать нашу анимацию
    @Override
    public Engine onCreateEngine() {
        try {
            return new NyanEngine();
        } catch (IOException e) {
            Log.w(TAG, "Error creating NyanEngine", e);
            stopSelf();
            return null;
        }
    }

    //само класс с анимауией
    class NyanEngine extends Engine {
        //объявляем переменные, Moview класс для отображения
        //анимаций как вы сам я думаю поняли. Остальные переменные
        //объявлялись для обределения продолжительности проигрыша
        //для runnuble для потока, дальше размер и начало и конец старта проигрыша
        private final Movie mNyan;
        private final int mNyanDuration;
        private final Runnable mNyanNyan;
        float mScaleX;
        float mScaleY;
        int mWhen;
        long mStart;

        // открываем файл из raw папки, декодируем его и смотрим насколько он
        //длительный по проигрышу
        NyanEngine() throws IOException {
            InputStream is = getResources().openRawResource(R.raw.nyan);
            if (is != null) {
                try {
                    mNyan = Movie.decodeStream(is);
                    mNyanDuration = mNyan.duration();
                } finally {
                    is.close();
                }
            } else {
                throw new IOException("Unable to open R.raw.nyan");
            }

            mWhen = -1;
            mNyanNyan = new Runnable() {
                public void run() {
                    nyan();
                }
            };
        }

        //уничтожаем поток если выходят из обоев
        @Override
        public void onDestroy() {
            super.onDestroy();
            mNyanHandler.removeCallbacks(mNyanNyan);
        }

        //если экран включен то запускаем, если выключен то тормазим
        //и ждем когда включится снова
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                nyan();
            } else {
                mNyanHandler.removeCallbacks(mNyanNyan);
            }
        }

        //создаем объект сюрфейса для отображения гифки, растягиваем ее во весь экран
        // и запускаем на экране
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mScaleX = width / (1f * mNyan.width());
            mScaleY = height / (1f * mNyan.height());
            nyan();
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep,
                                     float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
            nyan();
        }

        //класс запускающий анимацию пройгрыша гифки
        void nyan() {
            //частота пройгрыша анимации циклим все.
            tick();
            //создаем сюрфейс с канвасом, в принципе как и в статьях в которых
            // мы созадвали игру, пока все стандартно
            SurfaceHolder surfaceHolder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    //запускаем отрисовку с помощью канвы
                    nyanNyan(canvas);
                }
             //по завершению удаляем все к чертовой бабушке
             // если это когда нибудь случиться конечно
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
            mNyanHandler.removeCallbacks(mNyanNyan);
            if (isVisible()) {
                // 25 - количество фпс, и в целом это количество фпс за секунду
                // то есть 1000 это секунда 1 секунда делим на 25 кадров
                mNyanHandler.postDelayed(mNyanNyan, 1000L/25L);
            }
        }

        //таймер для заасечения времени проигрыша
        void tick() {
            if (mWhen == -1L) {
                mWhen = 0;
                mStart = SystemClock.uptimeMillis();
            } else {
                long mDiff = SystemClock.uptimeMillis() - mStart;
                mWhen = (int) (mDiff % mNyanDuration);
            }
        }

        // рисуем нашего кода на канвасе
        void nyanNyan(Canvas canvas) {
            canvas.save();
            //растягиваем на весь экран
            canvas.scale(mScaleX, mScaleY);
            //указываем частоту проигрыша
            mNyan.setTime(mWhen);
            //устанавливаем в левый верхний угол картинку и рисуем ее
            mNyan.draw(canvas, 0, 0);
            canvas.restore();
        }
    }
}