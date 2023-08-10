package com.camera.scanner;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class CameraWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.camera_widget_layout);

        // Create an intent to launch the camera activity
        Intent openCameraIntent = new Intent(context, CameraActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openCameraIntent, PendingIntent.FLAG_IMMUTABLE);

        // Attach the click event to the button
        views.setOnClickPendingIntent(R.id.cameraWidgetButton, pendingIntent);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}