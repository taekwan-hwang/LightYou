package com.skplanetx.tmapopenmapapi.ui;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.skplanetx.tmapopenmapapi.R;

public class NewActivity extends Activity {

    myDBHelper myHelper;
    TextView textViewNameResult, textViewLatResult, textViewLonResult;
    SQLiteDatabase sqlDB;

    Button bt_init;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        setTitle("즐겨찾기");

        textViewNameResult = (TextView) findViewById(R.id.textViewNameResult);
        textViewLatResult = (TextView) findViewById(R.id.textViewLatResult);
        textViewLonResult = (TextView) findViewById(R.id.textViewLonResult);

        myHelper = new myDBHelper(this);

        print(sqlDB, myHelper, textViewNameResult, textViewLatResult, textViewLonResult);

        bt_init = (Button) findViewById(R.id.bt_init);
        bt_init.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {

                sqlDB = myHelper.getWritableDatabase();
                myHelper.onUpgrade(sqlDB, 1, 2);
                sqlDB.close();
                print(sqlDB, myHelper, textViewNameResult, textViewLatResult, textViewLonResult);
            }
        });

    }

    public class myDBHelper extends SQLiteOpenHelper
    {

        public myDBHelper(Context context)
        {
            super(context, "bookmarkDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL("CREATE TABLE bookmarkTBL(gName CHAR(20),gLat INTEGER, gLon INTEGER);");

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            db.execSQL("DROP TABLE IF EXISTS bookmarkTBL");
            onCreate(db);

        }
    }

    public static void print(SQLiteDatabase sqlDB, myDBHelper myHelper, TextView textViewNameResult, TextView textViewLatResult, TextView textViewLonResult)
    {
        sqlDB = myHelper.getReadableDatabase();
        Cursor cursor;
        cursor = sqlDB.rawQuery("SELECT * FROM bookmarkTBL;", null);

        String strNames = "장소" + "\r\n" + "________" + "\r\n";
        String strLat = "위도" + "\r\n" + "________" + "\r\n";
        String strLon = "경도" + "\r\n" + "________" + "\r\n";

        while (cursor.moveToNext())
        {
            strNames += cursor.getString(0) + "\r\n";
            strLat += cursor.getString(1) + "\r\n";
            strLon += cursor.getString(2) + "\r\n";
        }

        textViewNameResult.setText(strNames);
        textViewLatResult.setText(strLat);
        textViewLonResult.setText(strLon);

        cursor.close();
        sqlDB.close();
    }
}