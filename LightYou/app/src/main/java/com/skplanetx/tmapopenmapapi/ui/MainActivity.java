package com.skplanetx.tmapopenmapapi.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapGpsManager;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;
import com.skplanetx.tmapopenmapapi.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends Activity implements TMapGpsManager.onLocationChangedCallback, TextToSpeech.OnInitListener  {

    TMapData tmapdata = null;
    private TMapGpsManager tmapgps = null;
    private TMapView tmapview = null;
    private static String mApiKey = "86ddd519-31b1-3bd3-97f1-fe95158cd5aa";

    /*
    private Button bt_find;
    private Button bt_fac;
    private Button bt_path;
    */
    private Button bt_current;
    private Button bt_init;
    private Button bt_say1;
    private Button bt_bookmark;

    String[] strNames = null;

    static int strNames_cnt = 0;

    TMapPoint point = null;

    private final int REQ_CODE_SPEECH_INPUT = 100;
    ArrayList<String> result = null;

    private TextToSpeech tts; // additional

    /*목적지까지 가는 좌표들의 집합*/
    ArrayList<TMapPoint> routes = new ArrayList<TMapPoint>();
    ArrayList<Integer> directionPoints = new ArrayList<>();

    private boolean m_bTrackingMode = true;
    double minLat,minLong,maxLat,maxLong;
    int routeCnt = 1;

    int flag = -1;
    String destination;
    DataOutputStream dos;

    /*ip, port, socket 설정*/
    private String ip = "35.200.60.107";
    private int port = 22222;
    private Socket socket = null;

    /*기능별 쓰레드 On/Off 여부*/
    private boolean isRecognizingThreadOn = false;
    private boolean isFindTargetThreadOn = false;

    //private Thread recMsgTh;
    //private Thread naviTh;
    //private Thread findTargetTh;
    private BufferedReader br;

    /*즐겨찾기 기능 관련*/
    myDBHelper myHelper;
    SQLiteDatabase sqlDB;
    String name = null;
    String latitude = null;
    String longitude = null;
    private String str_temp1 = null;
    private String str_temp2 = null;
    private String str_temp3 = null;

    /*물건 찾기  타겟 변수*/
    private String target = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        bt_find = (Button) findViewById(R.id.bt_findadd);
        bt_fac = (Button) findViewById(R.id.bt_findfac);
        bt_path = (Button) findViewById(R.id.bt_findpath);
        */
        bt_current = (Button) findViewById(R.id.bt_current);

        /*즐겨찾기*/
        bt_bookmark = (Button) findViewById(R.id.bt_bookmark);
        bt_bookmark.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), NewActivity.class);
                startActivity(intent);
            }
        });


        /* additional */
        tts = new TextToSpeech(this, this);

        tmapdata = new TMapData();
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.mapview);
        tmapview = new TMapView(this);
        tmapview.setSKPMapApiKey(mApiKey);
        tmapview.setCompassMode(true);
        tmapview.setIconVisibility(true);
        tmapview.setZoomLevel(15);
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setTrackingMode(true);
        tmapview.setSightVisible(true);
        //showMarkerPoint();
        /*지도의 중심좌표 위치 모드를 네비게이션 모드로 설정*/
        //tmapview.setMapPosition(TMapView.POSITION_NAVI);

        tmapgps = new TMapGpsManager(MainActivity.this);
        tmapgps.setMinTime(500);
        tmapgps.setMinDistance(1);

        /*스마트폰에서 네트워크 연결 시 (실내에서는 이게 유용)*/
        //tmapgps.setProvider(tmapgps.NETWORK_PROVIDER);

        /*스마트폰의 gps 사용 시 */
        tmapgps.setProvider(tmapgps.GPS_PROVIDER);

       /* Thread gpsTh = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(4000);
                        if (tmapgps.getSatellite() <= 2){
                            tmapgps.setProvider(tmapgps.NETWORK_PROVIDER);
                            Log.d("gps", "2개 이하일때 (실내)");
                        }
                        else{
                            tmapgps.setProvider(tmapgps.GPS_PROVIDER);
                            Log.d("gps", "3개 이상일때 (실외)");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });*/
        //gpsTh.start();

        /*gps 시작*/
        tmapgps.OpenGps();

        /*현재 나의 위치로 지도의 중심을 이동시킨다.*/
        TMapPoint curPoint = tmapgps.getLocation();
        tmapview.setCenterPoint(curPoint.getLatitude(), curPoint.getLongitude());

        bt_current.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrentGps();
            }
        });

        //레이아웃에 티맵 부착
        linearLayout.addView(tmapview);

        /* SAY 버튼 */
        bt_say1 = (Button)findViewById(R.id.bt_say);
        bt_say1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                routine();
            }
        });

        //서버 접속
        Thread connTh = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ip, port);
                    //br = null;
                    // while(br.equals(null))
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeByte('1');
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        connTh.start();
    }

    public void setCurrentGps()
    {
        tmapview.setTrackingMode(true);
        tmapview.setCompassMode(true);
        tmapview.setSightVisible(true);
        tmapview.setIconVisibility(true);

        /*현재 나의 위치로 지도의 중심을 이동시킨다.*/
        TMapPoint curPoint = tmapgps.getLocation();
        Log.d("위치", ""+curPoint.getLatitude() + ", " + curPoint.getLongitude());

        tmapview.setCenterPoint(curPoint.getLatitude(), curPoint.getLongitude());
        tmapview.setMapPosition(TMapView.POSITION_NAVI);

        //디버깅용 (나중에 지울 것)
        //routine();
    }

    @Override
    public void onLocationChange(Location location) {

        if(flag==2)
        {
            drawPedestrianPath();
            tmapgps.setMinDistance(3);
            flag = -1;
        }

        if (m_bTrackingMode) {
            int i = 0;
            tmapview.setLocationPoint(location.getLongitude(), location.getLatitude());
            Toast.makeText(getApplicationContext(),"변화감지",Toast.LENGTH_SHORT).show();

            //수정한곳, 경로가 결정되고, routeCnt가 총 경유좌표의 개수보다 작을 때 실행코드
            if(routes.size()>0 && routeCnt<routes.size()) {
                double diffLatitude = LatitudeInDifference(7);
                if(diffLatitude<0)
                {
                    diffLatitude *= -1;
                }
                double diffLongitude = LongitudeInDifference(routes.get(routeCnt).getLongitude(), 10);
                if(diffLongitude<0)
                {
                    diffLongitude *= -1;
                }

                minLat= routes.get(routeCnt).getLatitude()-diffLatitude;
                minLong= routes.get(routeCnt).getLongitude()-diffLongitude;
                maxLat= routes.get(routeCnt).getLatitude()+diffLatitude;
                maxLong=routes.get(routeCnt).getLongitude()+diffLongitude;

                if (minLat <= location.getLatitude() && location.getLatitude() <= maxLat && minLong <= location.getLongitude() && location.getLongitude() <= maxLong) {
                    Toast.makeText(getApplicationContext(),""+directionPoints.get(routeCnt),Toast.LENGTH_SHORT).show();
                    //안내 시, 다음 좌표와의 거리가 있는 경우.
                    if(routeCnt+1<routes.size())
                    {
                        int distan =(int)distance(routes.get(routeCnt).getLatitude(), routes.get(routeCnt).getLongitude(), routes.get(routeCnt+1).getLatitude(), routes.get(routeCnt+1).getLongitude(), "meter");
                        distan/=10;
                        distan*=10;
                        //직진인 경우
                        if(directToString(directionPoints.get(routeCnt))=="직진")
                            speakOut(distan+"미터"+directToString(directionPoints.get(routeCnt)));
                            //직진 이외의 경우
                        else
                            speakOut(directToString(directionPoints.get(routeCnt))+"그리고"+distan+"미터 직진하세요");
                    }
                    //목적지 인 경우
                    else
                        speakOut(directToString(directionPoints.get(routeCnt)));

                    Log.d("Zz",""+minLat+","+minLong+"  "+maxLat+","+maxLong);
                    Log.d("Zz",""+location.getLatitude()+","+location.getLongitude());

                    routeCnt++;
                }
            }

        }

    }

    public void convertToAddress(final String strData) {
        if(flag == 3)
        {
            String str = strData;
            String[] names = str.split("을|를|으로|로");
            str_temp1 = names[0].trim();
            str_temp2 = names[1].trim();
            str_temp3 = names[2].trim();
            name = str_temp1;
            Log.d("park", "원래 문장 : " + strData);
            Log.d("park", "str_temp1 : " + str_temp1);
            Log.d("park", "str_temp2 : " + str_temp2);
            TMapData tmapdata = new TMapData();
            tmapdata.findAllPOI(str_temp1, new TMapData.FindAllPOIListenerCallback() {
                @Override
                public void onFindAllPOI(ArrayList<TMapPOIItem> poiItem) {
                    for (int i = 0; i < poiItem.size(); i++) {
                        TMapPOIItem item = poiItem.get(i);//

                        Log.d("주소로찾기", "POI Name : " + item.getPOIName().toString() + ", " + "Address: " + item.getPOIAddress().replace("null", "") + ", " + "Point: " + item.getPOIPoint().toString());

                        if (i == 0) {
                            tmapview.setCenterPoint(Double.parseDouble(item.noorLat), Double.parseDouble(item.noorLon));
                            point = item.getPOIPoint();

                            latitude = item.noorLat;
                            longitude = item.noorLon;

                            myHelper = new myDBHelper(MainActivity.this);
                            sqlDB = myHelper.getWritableDatabase();
                            sqlDB.execSQL("INSERT INTO bookmarkTBL VALUES ('" + str_temp2 + "' , " + latitude + ", " + longitude + ");");
                            sqlDB.close();
                            speakOut("성공적으로 추가되었습니다.");
                            //이거넣으면안됨   Toast.makeText(MainActivity.this, "입력되었습니다.", Toast.LENGTH_LONG).show();
                            print(sqlDB, myHelper);
                            flag = -1;
                        }
                    }
                }
            });
        }

        else
        {
            TMapData tmapdata = new TMapData();
            tmapdata.findAllPOI(strData, new TMapData.FindAllPOIListenerCallback() {
                @Override
                public void onFindAllPOI(ArrayList<TMapPOIItem> poiItem) {
                    for (int i = 0; i < poiItem.size(); i++) {
                        TMapPOIItem item = poiItem.get(i);//

                        Log.d("주소로찾기", "POI Name : " + item.getPOIName().toString() + ", " + "Address: " + item.getPOIAddress().replace("null", "") + ", " + "Point: " + item.getPOIPoint().toString());

                        if (i == 0) {
                            tmapview.setCenterPoint(Double.parseDouble(item.noorLat), Double.parseDouble(item.noorLon));
                            point = item.getPOIPoint();
                        }
                    }
                }
            });
            if (flag == 1) {
                speakOut("경로안내를 시작할게요.");
                speakOut("다음 안내까지는 계속 직진하겠습니다.");
                tmapgps.setMinDistance((float) 0.5);
                flag = 2;
                // drawPedestrianPath();
            }
        }
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    /* routine() 에서 음성 인식이 성공했을 경우 여기서 각 기능 실행*/
                    String input = result.get(0);
                    Toast.makeText(this, input, Toast.LENGTH_SHORT).show();

                    if(flag==3)
                    {
                        convertToAddress(result.get(0));
                    }

                    //
                    if(flag==0)
                    {
                        Log.d("zzds","dfs"+destination);
                        destination = new String(result.get(0));
                    /*
                    char lastName = result.get(0).charAt(name.length() - 1);

                    String s = "가";
                    if ((lastName - 0xAC00) % 28 > 0) {
                        s = "가";
                    }
                    else{
                        s = "이";
                    }

                    */
                        speakOut(result.get(0) +  "  맞습니까?");
                    /*try {
                        Thread.sleep(1500 + result.get(0).length() * 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                        flag=1;
                        promptSpeechInput();

                    }
                    else if(flag==1)
                    {
                        if(result.get(0).matches(".*맞아요.*")||result.get(0).matches(".*맞습니다.*")|| result.get(0).matches(".*맞아.*") || result.get(0).matches(".*마자.*")) {

                            String[][] temp;
                            myHelper = new myDBHelper(MainActivity.this);
                            sqlDB = myHelper.getWritableDatabase();
                            print(sqlDB, myHelper);
                            temp = getLocationInformation(sqlDB, myHelper, destination);
                            if(temp[0][0] != null && temp[0][1] != null)
                            {
                                point = null;
                                Double lat = Double.parseDouble(temp[0][0]);
                                Double lon = Double.parseDouble(temp[0][1]);

                                point = new TMapPoint(lat, lon);

                                speakOut("경로안내를 시작할게요.");
                                speakOut("다음 안내까지는 계속 직진하겠습니다.");
                                drawPedestrianPath();
                                flag=-1;
                            }
                            else
                            {
                                convertToAddress(destination);
                            }
                        }
                        else
                        {
                            speakOut("목적지를 다시 말해주세요");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            flag=0;
                            promptSpeechInput();
                        }
                    }
                    //
                    // 1. history
                    if (input.matches(".*즐겨찾기.*") || input.matches(".*북마크.*")){
                        flag = 3;
                        speakOut("숭실대를 우리학교로 추가해줘와 같이 말하세요");
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        promptSpeechInput();
                    }

                    // 2. 내 위치
                    else if (input.matches(".*내 위치.*") || input.matches(".*내위치.*") || input.matches(".*위치.*")){
                        TMapPoint myPoint = tmapgps.getLocation();
                        tmapdata.convertGpsToAddress(myPoint.getLatitude(), myPoint.getLongitude(), new TMapData.ConvertGPSToAddressListenerCallback() {
                            @Override
                            public void onConvertToGPSToAddress(String s) {
                                speakOut("현재 위치는 "+s+" 입니다.");
                            }
                        });
                    }

                    // 3. 네비게이션
                    else if (input.matches(".*길 안내.*") || input.matches(".*길안내.*") || input.matches(".*안내.*") || input.matches(".*가는길.*") || input.matches(".가는 길.*")){

                        try {
                            speakOut("목적지를 말해주세요");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            flag = 0;
                            promptSpeechInput();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                    // 4. 전방인식
                    else if (input.matches(".*전방 안내 해줘.*") || input.matches(".*전방 안내.*") || input.matches(".*전방안내.*") || input.matches(".*전방.*")){
                        //서버로부터 데이터를 받는 쓰레드 정의
                        if (!isRecognizingThreadOn) {
                            speakOut("전방 안내를 시작할게요.");
                            isRecognizingThreadOn = true;
                            Log.e("recMsgTh","before start");
                            recMsgTh.start();
                        } else if (isRecognizingThreadOn) {
                            speakOut("이미 실행 중입니다.");
                        }
                    } else if (input.matches(".*이제 됐어.*") || input.matches(".*이제됐어.*")){
                        //서버로부터 데이터를 받는 쓰레드 정의
                        if (isRecognizingThreadOn) {
                            Log.e("recMsgTh","before interrupt");
                            isRecognizingThreadOn = false;
                            speakOut("전방 안내를 종료할게요.");
                        } else if (!isRecognizingThreadOn) {
                            speakOut("실행되어 있지 않습니다.");
                        }
                    }

                    // 5. 물건 찾기
                    else if (input.matches(".*찾아.*") || input.matches(".*어디있어.*") || input.matches(".*어디 있어.*")){
                        String[] tmp = null;
                        if (input.matches(".*찾아.*")){
                            tmp = input.split("찾아");
                        }
                        else if (input.matches(".*어디.*")){
                            tmp = input.split("어디");
                        }
                        target = tmp[0];
                        if (target.matches(".*까요.*")) {
                            String[] realTarget = target.split("까요");
                            target = realTarget[1].trim();
                        }
                        isFindTargetThreadOn = true;
                        speakOut(target + "찾기를 시작합니다.");
                        findTargetTh.start();
                    } else if (input.matches(".*그만 찾기.*")){
                        speakOut(target + "찾기를 종료합니다.");
                        isFindTargetThreadOn = false;
                    }

                    //6. 현재 시간 알려주기
                    else if (input.matches(".*몇 시.*") || input.matches(".*몇시.*") || input.matches(".*시간.*")){
                        Calendar calendar = Calendar.getInstance();
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        int minute = calendar.get(Calendar.MINUTE);
                        String hourInKorean = "";
                        switch (hour){
                            case 0 : hourInKorean = "오전 열두"; break;
                            case 1 : hourInKorean = "오전 한"; break;
                            case 2 : hourInKorean = "오전 두"; break;
                            case 3 : hourInKorean = "오전 세"; break;
                            case 4 : hourInKorean = "오전 네"; break;
                            case 5 : hourInKorean = "오전 다섯"; break;
                            case 6 : hourInKorean = "오전 여섯"; break;
                            case 7 : hourInKorean = "오전 일곱"; break;
                            case 8 : hourInKorean = "오전 여덟"; break;
                            case 9 : hourInKorean = "오전 아홉"; break;
                            case 10 : hourInKorean = "오전 열"; break;
                            case 11 : hourInKorean = "오전 열 한"; break;
                            case 12 : hourInKorean = "오후 열 두"; break;
                            case 13 : hourInKorean = "오후 한"; break;
                            case 14 : hourInKorean = "오후 두"; break;
                            case 15 : hourInKorean = "오후 세"; break;
                            case 16 : hourInKorean = "오후 네"; break;
                            case 17 : hourInKorean = "오후 다섯"; break;
                            case 18 : hourInKorean = "오후 여섯"; break;
                            case 19 : hourInKorean = "오후 일곱"; break;
                            case 20 : hourInKorean = "오후 여덟"; break;
                            case 21 : hourInKorean = "오후 아홉"; break;
                            case 22 : hourInKorean = "오후 열"; break;
                            case 23 : hourInKorean = "오후 열 한"; break;
                        }
                        String resultText = "지금 시각은 " + hourInKorean + "시 " + String.valueOf(minute) + " 분입니다. ";
                        speakOut(resultText);
                    }

                    else if(input.matches(".*목록.*") || input.matches(".*뭐 있어.*") || input.matches(".*뭐있어.*"))
                    {
                        //String[] strNames = null;
                        speakOut("즐겨찾기 목록은 다음과 같습니다. ");
                        Log.d("strTTTT", "헬퍼전");
                        myHelper = new myDBHelper(this);
                        strNames = print(sqlDB, myHelper);

                        for(int i = 0; i < strNames_cnt; i++)
                        {
                            try {
                                Thread.sleep(1700);
                                speakOut(strNames[i]);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }

                break;
            }
        }

        Toast.makeText(this, result.get(0), Toast.LENGTH_SHORT).show();
    }



    /* 보행자 경로 */
    public void drawPedestrianPath() {
        TMapPoint point1 = tmapview.getCenterPoint();
        TMapPoint point2 = point;
        TMapData tmapdata = new TMapData();
        Document doc = null;

        /*XML 문서 가져오기*/
        try {
            doc = tmapdata.findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, point1, point2);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        /*XML 파싱하기*/
        Element root = doc.getDocumentElement();
        NodeList nodeListPlacemark = root.getElementsByTagName("Placemark");
        for( int i=0; i<nodeListPlacemark.getLength(); i++ ) {
            NodeList nodeListPlacemarkItem = nodeListPlacemark.item(i).getChildNodes();
            for( int j=0; j<nodeListPlacemarkItem.getLength(); j++ ) {
                if( nodeListPlacemarkItem.item(j).getNodeName().equals("tmap:turnType") ) {
                    Log.d("debug", nodeListPlacemarkItem.item(j).getTextContent().trim() );
                    int tmpPoint = Integer.parseInt(nodeListPlacemarkItem.item(j).getTextContent().trim());
                    directionPoints.add(tmpPoint);
                }
                if ( nodeListPlacemarkItem.item(j).getNodeName().equals("Point") ){
                    Log.d("점 좌표", nodeListPlacemarkItem.item(j).getTextContent().trim());
                    String[] strTmp = nodeListPlacemarkItem.item(j).getTextContent().trim().split(",");
                    TMapPoint t = new TMapPoint(Double.parseDouble(strTmp[1]), Double.parseDouble(strTmp[0]));
                    routes.add(t);
                    Log.d("개수",""+routes.size());
                }
            }
        }

        //지도 그리기
        tmapdata.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, point1, point2, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine polyLine) {
                polyLine.setLineColor(Color.BLUE);
                tmapview.addTMapPath(polyLine);
                //dis = polyLine.getDistance();
            }
        });

    }

    /* additional */
    @Override
    public void onDestroy() {
        // Don't forget to shutdown!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    /* tts 초기화*/
    @Override
    public void onInit(int status)
    {
        if (status == TextToSpeech.SUCCESS)
        {
            int result = tts.setLanguage(Locale.KOREA);
            // tts.setPitch(5); // set pitch level
            tts.setSpeechRate((float) 1.2); // set speech speed rate
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Log.e("TTS", "Language is not supported");
            }
            else
            {
                //btnSpeak.setEnabled(true);
            }
        }
        else {
            Log.e("TTS", "Initilization Failed");
        }
    }

    /* 스트링을 음성으로 바꾸기*/
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
    } //QUEUE_ADD

    //정보를 스트링으로 바꾸는 함수
    public String directToString(int input)
    {
        String res = null;
        switch (input){
            case 11 : res = "직진";
                break;
            case 12 : res = "왼쪽으로 가세요.";
                break;
            case 13 : res = "오른쪽으로 가세요.";
                break;
            case 14 : res = "유턴하세요.";
                break;
            case 16 : res = "8시 방향 왼쪽으로 가세요.";
                break;
            case 17 : res = "10시 방향 왼쪽으로 가세요.";
                break;
            case 18 : res = "2시 방향 오른쪽으로 가세요.";
                break;
            case 19 : res = "4시 방향 오른쪽으로 가세요.";
                break;
            case 125 : res = "전방에 있는 육교를 지나가세요.";
                break;
            case 126 : res = "전방에 있는 지하보도를 지나가세요.";
                break;
            case 127 : res = "전방에 있는 계단을 통해 지나가세요.";
                break;
            case 128 : res = "전방에 경사로가 있으니 조심히 지나가세요.";
                break;
            case 211 : res = "전방에 있는 횡단보도를 지나가세요.";
                break;
            case 212 : res = "좌측 횡단보도를 건너가세요.";
                break;
            case 213 : res = "우측 횡단보도를 건너가세요.";
                break;
            case 214 : res = "8시 방향 횡단보도를 건너가세요.";
                break;
            case 215 : res = "10시 방향 횡단보도를 건너가세요.";
                break;
            case 216 : res = "2시 방향 횡단보도를 건너가세요.";
                break;
            case 217 : res = "4시 방향 횡단보도를 건너가세요.";
                break;
            case 218 : res = "전방에 있는 엘리베이터를 이용하세요.";
                break;
            case 200 : res = "출발지";
                break;
            case 201 : res = "목적지에 도착했습니다.고생 많으셨습니다.";
                break;
            default: res = "직진하세요";
        }
        return res;
    }

    //거리 구할 때 사용하는 함수
    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        if (unit == "kilometer") {
            dist = dist * 1.609344;
        } else if(unit == "meter"){
            dist = dist * 1609.344;
        }

        return (dist);
    }
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    //반경 m이내의 위도차(degree)
    public double LatitudeInDifference(int diff){
        //지구반지름
        final int earth = 6371000;    //단위m

        return (diff*360.0) / (2*Math.PI*earth);
    }

    //반경 m이내의 경도차(degree)
    public double LongitudeInDifference(double _latitude, int diff){
        //지구반지름
        final int earth = 6371000;    //단위m

        double ddd = Math.cos(0);
        double ddf = Math.cos(Math.toRadians(_latitude));

        return (diff*360.0) / (2*Math.PI*earth*Math.cos(Math.toRadians(_latitude)));
    }

    /*
 영->한 결과 매칭
            java7에서는 switch 문에서 String 타입 표현식을 지원하므로 switch 구현
  */
    public String translator(String eng){
        String kor = "";
        if(eng.equals(null))
            return "null";

        switch(eng){
            case "computer keyboard":
                kor = "키보드";
                break;
            case "computer mouse":
                kor = "마우스";
                break;
            case "computer monitor":
                kor = "모니터";
                break;
            case "backpack":
                kor = "가방";
                break;
            case "wine bottle":
                kor = "와인병";
                break;
            case "paperclip":
                kor = "종이 클립";
                break;
            case "eyeglasses":
                kor = "안경";
                break;
            case "chair":
                kor = "의자";
                break;
            case "face":
                kor = "얼굴";
                break;
            case "wheelchair":
                kor = "휠체어";
                break;
            case "scissors":
                kor = "가위";
                break;
            case "necktie":
                kor = "넥타이";
                break;
            case "box":
                kor = "박스";
                break;
            case "socks":
                kor = "양말";
                break;
            case "knife":
                kor = "칼";
                break;
            case "t shirt":
                kor = "티셔츠";
                break;
            case "calculator":
                kor = "계산기";
                break;
            case "mattress bed":
                kor = "침대";
                break;
            case "refrigerator":
                kor = "냉장고";
                break;
            case "headphone":
                kor = "헤드폰";
                break;
            case "stapler":
                kor = "스테이플러";
                break;
            case "hamburger":
                kor = "햄버거";
                break;
            case "soda can":
                kor = "음료수 캔";
                break;
            case "spoon":
                kor = "숟가락";
                break;
            case "frying pan":
                kor = "프라이팬";
                break;
            case "shoes":
                kor = "신발";
                break;
            case "umbrella":
                kor = "우산";
                break;
            case "laptop":
                kor = "노트북";
                break;
            case "library":
                kor = "도서관";
                break;
            case "fire extinguisher":
                kor = "소화기";
                break;
            case "watch":
                kor = "시계";
                break;
            case "cup":
                kor = "컵";
                break;
            case "block":
                kor = "고층건물";
                break;
            case "bridge":
                kor = "다리";
                break;
            case "car":
                kor = "자동차";
                break;
            case "cornor":
                kor = "꺾이는 길";
                break;
            case "flower":
                kor = "꽃";
                break;
            case "road":
                kor = "차도";
                break;
            case "sidewalk":
                kor = "보도";
                break;
            case "telephone pole":
                kor = "전봇대";
                break;
            case "toilet":
                kor = "화장실";
                break;
            case "tree":
                kor = "나무";
                break;
            case "truck":
                kor = "트럭";
                break;
            case "ramp":
                kor = "경사로";
                break;
            case "knob":
                kor = "손잡이";
                break;
            case "elevator":
                kor = "엘리베이터";
                break;
            case "stair":
                kor = "계단";
                break;
            case "people":
                kor = "사람";
                break;
            case "사람":
                kor = "people";
                break;
            case "키보드":
                kor = "computer keyboard";
                break;
            case "마우스":
                kor = "computer mouse";
                break;
            case "모니터":
                kor = "computer monitor";
                break;
            case "가방":
                kor = "backpack";
                break;
            case "와인병":
                kor = "wine bottle";
                break;
            case "종이 클립":
            case "종이클립":
                kor = "paperclip";
                break;
            case "안경":
                kor = "eyeglasses";
                break;
            case "의자":
                kor = "chair";
                break;
            case "얼굴":
                kor = "face";
                break;
            case "휠체어":
                kor = "wheelchair";
                break;
            case "가위":
                kor = "scissors";
                break;
            case "넥타이":
                kor = "necktie";
                break;
            case "박스":
                kor = "box";
                break;
            case "양말":
                kor = "socks";
                break;
            case "칼":
                kor = "knife";
                break;
            case "티셔츠":
                kor = "t shirt";
                break;
            case "계산기":
                kor = "calculator";
                break;
            case "침대":
                kor = "mattress bed";
                break;
            case "냉장고":
                kor = "refrigerator";
                break;
            case "헤드폰":
                kor = "headphone";
                break;
            case "스테이플러":
                kor = "stapler";
                break;
            case "햄버거":
                kor = "hamburger";
                break;
            case "음료수 캔":
            case "음료수캔":
                kor = "soda can";
                break;
            case "숟가락":
                kor = "spoon";
                break;
            case "프라이팬":
                kor = "frying pan";
                break;
            case "신발":
                kor = "shoes";
                break;
            case "우산":
                kor = "umbrella";
                break;
            case "노트북":
                kor = "laptop";
                break;
            case "도서관":
                kor = "library";
                break;
            case "소화기":
                kor = "fire extinguisher";
                break;
            case "시계":
                kor = "watch";
                break;
            case "컵":
                kor = "cup";
                break;
            case "고층건물":
                kor = "block";
                break;
            case "다리":
                kor = "bridge";
                break;
            case "자동차":
                kor = "car";
                break;
            case "꺾이는 길":
            case "꺾이는길":
            case "코너":
                kor = "cornor";
                break;
            case "꽃":
                kor = "flower";
                break;
            case "차도":
                kor = "road";
                break;
            case "보도":
                kor = "sidewalk";
                break;
            case "전봇대":
                kor = "telephone pole";
                break;
            case "화장실":
                kor = "toilet";
                break;
            case "나무":
                kor = "tree";
                break;
            case "트럭":
                kor = "truck";
                break;
            case "경사로":
                kor = "ramp";
                break;
            case "손잡이":
                kor = "knob";
                break;
            case "엘리베이터":
                kor = "elevator";
                break;
            case "계단":
                kor = "stair";
                break;
            default:
                return eng;
        }
        return kor;
    }

    //각 기능별 구현부
    public void routine() {
        speakOut("무엇을 도와드릴까요?");

        //음성 입력
        promptSpeechInput();
    }


    Thread  recMsgTh = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                String readData;
                String previousData = "starting";
                while (isRecognizingThreadOn)
                {
                    long startTime = System.currentTimeMillis();
                    Log.e("time","" + startTime);
                    while(false||(System.currentTimeMillis()-startTime)<5000)
                        readData = br.readLine();
                    Log.e("time","" + (System.currentTimeMillis()-startTime));
                    Log.e("recMsgTh","run.after br");
                    readData = br.readLine();
                    Log.e("recMsgTh","run.in if br : " + readData);
                    Log.d("Data from server", readData);
                    Log.d("recMsgTh", readData);
                    readData = translator(readData);
                    if(!previousData.equals(readData)) {
                        //if(readData.equals("에러"))
                        //    continue;
                        speakOut("전방에" + readData + " 있습니다.");
                    }
                    previousData = readData;
//                    speakOut("전방에 " + readData + "가 있습니다.");
                    // Thread.sleep(10000);
                }
            } catch (Exception e) {
                Log.e("recMsgTh","run.in exception");
                speakOut("전방 예외로 끝.");
                e.printStackTrace();
            }
        }
    });

    Thread findTargetTh = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                String readData,temp;
                while (isFindTargetThreadOn)
                {
                    /* readData : 서버로부터 받아온 영문 결과값
                       target : 안드로이드가 받아온 찾으려는 한글 물체명 -> 영문 변환
                       temp : 안드로이드가 받아온 찾으려는 한글 물체명
                     */
                    readData = br.readLine();

                    Log.d("DataFromServer", "데이터는"+readData+"이다");
                    temp = target;
                    target = translator(target);
                    Log.d("DataFromServer", "데이터는"+target+"이다");
                    if(target.matches(temp)){
                        speakOut(target + "찾을 수 없는 물건입니다");
                        break;
                    }

                    //인식된 대상이 내가 찾고자 하는 물건과 일치할 경우
                    if (target.equals(readData)){
                        speakOut(temp + "을 찾았습니다.");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    public class myDBHelper extends SQLiteOpenHelper {

        public myDBHelper(Context context) {
            super(context, "bookmarkDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE bookmarkTBL(gName CHAR(20),gLat INTEGER, gLon INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS bookmarkTBL");
            onCreate(db);
        }
    }
    public static String[] print(SQLiteDatabase sqlDB, MainActivity.myDBHelper myHelper) {
        Log.d("strTTTT", "넘어옴");
        sqlDB = myHelper.getReadableDatabase();
        Log.d("strTTTT", "넘어옴222");
        Cursor cursor;
        cursor = sqlDB.rawQuery("SELECT * FROM bookmarkTBL;", null);
        Log.d("strTTTT", "넘어옴3333");
        strNames_cnt = 0;
        String strNames[] = new String[100];
        int i = 0;
        while (cursor.moveToNext()) {

            strNames_cnt++;
            strNames[i] = cursor.getString(0);
            Log.d("strTTTT", "값 : " + strNames_cnt);
            Log.d("strTTTT", "커서 : " + cursor.getString(0));
            i++;

        }
        cursor.close();
        sqlDB.close();

        Log.d("strTTTT", "잘끝냄");

        /*for(i=0; i<strNames_cnt; i++)
        {
            strNames[i] = temp[i];
            Log.d("strTTTT", "strName " + strNames[i]);
        }*/

        Log.d("strTTTT", "끝까지끝");

        return strNames;
    }


    public static String[][] getLocationInformation(SQLiteDatabase sqlDB, MainActivity.myDBHelper myHelper, String strName)
    {
        sqlDB = myHelper.getReadableDatabase();
        Cursor cursor;
        cursor = sqlDB.rawQuery("SELECT * FROM bookmarkTBL;", null);
        String[][] locationInformation = new String[1][2];
        locationInformation[0][0] = null;
        locationInformation[0][1] = null;
        Log.d("chchchch",""+5);

        while (cursor.moveToNext()) {

            if(strName.equals(cursor.getString(0)))
            {
                locationInformation[0][0] = cursor.getString(1);
                locationInformation[0][1] = cursor.getString(2);

                Log.d("chchchch",""+6);
                break;

            }
        }
        Log.d("chchchch",""+7);
        cursor.close();
        sqlDB.close();

        Log.d("strTTTT", "잘끝냄");

        return  locationInformation;
    }

}
