package com.massey.a3.dailyvibe.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Post.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class PostDatabase extends RoomDatabase {
    public abstract PostDao postDao();

    private static volatile PostDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static PostDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PostDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            PostDatabase.class, "post_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
