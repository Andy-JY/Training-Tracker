package com.choubapp.running;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.security.AccessController.getContext;

public class CoachReportsActivity extends AppCompatActivity {
    ListView lv; LinearLayout Tdata;
    RelativeLayout loading;
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference Teams = db.collection("Equipe");
    CollectionReference Trainings = db.collection("Entrainement");
    CollectionReference Trackings = db.collection("tracking");
    String ID;
    HashMap<Timestamp,String> EndedTrainings ;
    Map<Timestamp, String> map ;
    ArrayList<Long> speed ;
    ArrayList<Integer> steps;
    long distance, time;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_reports);
        lv = findViewById(R.id.listView1);
        Tdata = findViewById(R.id.TeamData);
        loading = findViewById(R.id.loading);
        //lv.setVisibility(View.INVISIBLE);
        //loading.setVisibility(View.GONE);
        LoadSpinnerData();
    }

    public void LoadSpinnerData(){
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        List<String> TeamsList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, TeamsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        Teams.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String teamname = document.getString("Nom Equipe");
                        String coachmail = document.getString("Email Coach");
                        if (teamname!=null && coachmail.equals(firebaseAuth.getCurrentUser().getEmail()) ){
                            TeamsList.add(teamname);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String team = parent.getSelectedItem().toString();
                lv.setVisibility(View.INVISIBLE);
                Tdata.setVisibility(View.INVISIBLE);
                getTeamID(team);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    public void getTeamID(String Tname){
        CollectionReference equipe = db.collection("Equipe");
        equipe.whereEqualTo("Nom Equipe", Tname).whereEqualTo("Email Coach", firebaseAuth.getCurrentUser().getEmail())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Log.d("TAG", document.getId() + " => " + document.getData());
                                ID =document.get("ID").toString();
                            }
                            getTeamTrainingsData(ID);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    private void getTeamTrainingsData(String Team){
        EndedTrainings = new HashMap<>();
        Trainings.whereEqualTo("TeamID",Team).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        String trainingName = document.getString("TrainingName");
                        if (trainingName!=null){
                            String mdate = document.get("Date").toString();
                            String mTimeDep = document.get("HeureDep").toString();
                            String mTimeArr = document.get("HeureArr").toString();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                            Date parsedDateDep=null;
                            Date parsedDateArr=null;
                            String DateDep = mdate +" "+mTimeDep;
                            String DateArr = mdate +" "+mTimeArr;
                            //System.out.println("Dates  "+DateDep+"  "+DateArr);

                            try {
                                parsedDateDep =(Date) dateFormat.parse(DateDep);
                                Timestamp timestampDep = new Timestamp(parsedDateDep.getTime());
                                parsedDateArr =(Date) dateFormat.parse(DateArr);
                                Timestamp timestampArr = new Timestamp(parsedDateArr.getTime());
                                //System.out.println("parsedDates  "+parsedDateDep+"  "+parsedDateArr);
                                //System.out.println("Timestamps  "+timestampDep+"  "+timestampArr);
                                Date datee= new Date();
                                Timestamp mytime = new Timestamp(datee.getTime());
                                if(mytime.after(timestampArr)){
                                    EndedTrainings.put(timestampArr,document.getId());
                                   // System.out.println("hashmap ended trainings"+EndedTrainings);
                                }

                            } catch(Exception e) {
                                e.printStackTrace();
                                System.out.println("Exception :" + e);
                            }
                        }
                    }getDataforCharts();
                }
            }
        });
    }
    private void getDataforCharts(){
        speed = new ArrayList<>();
        steps = new ArrayList<>();
        distance=0;
        time=0;
        map = new TreeMap<Timestamp, String>(EndedTrainings);
        System.out.println("treemap ended trainings sorted"+map);
        System.out.println("map size: " + map.size());
        for (int j =0 ; j<map.size(); j++) {
            speed.add((long) 0);
            steps.add(0);
        }
        System.out.println("steps "+ steps + " speed "+ speed);
        for (int i=0 ; i<map.size(); i++){
            final int[] counter = {0};
            Timestamp key = (Timestamp) map.keySet().toArray()[i];
            String docID= map.get(key);
            int finalI = i;
            Trackings.document(docID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("TAG", "Document exists!");
                            Trackings.document(docID).collection("Participants").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()){
                                        long dist=0, tm=0;
                                        for (DocumentSnapshot DOC : task.getResult()){
                                            counter[0]++;
                                            long sp = (Long.parseLong( DOC.get("Distance").toString())/ Long.parseLong(DOC.get("TotalTime").toString()));
                                            System.out.println("speed" +"  "+sp);
                                            speed.set(finalI, (long) (speed.get(finalI)+sp));
                                            long stp = (long) DOC.get("Steps");
                                            steps.set(finalI,steps.get(finalI)+(int)stp);
                                            dist+= (long) DOC.get("Distance");
                                            tm+=(long)DOC.get("TotalTime");

                                            System.out.println("participant " +finalI+" speed "+speed.get(finalI)+" steps "+steps.get(finalI)+" dist "+dist+ " time "+ tm);
                                        }
                                        distance += dist / counter[0];
                                        time += tm / counter[0];
                                        System.out.println("average distance "+distance + "avg time "+ time + "while counter = "+ counter[0]);
                                        if (finalI == (map.size()-1) && task.isComplete()) {
                                            Handler handler = new Handler();
                                            handler.postDelayed(new Runnable() {
                                                public void run() {
                                                    generateCharts();
                                                    loading.setVisibility(View.GONE);
                                                }
                                            }, 2000);
                                        }
                                    }
                                    System.out.println("speed of training n° "+finalI + "is "+ speed.get(finalI) + "and avg steps  = "+ steps.get(finalI));
                                    speed.set(finalI, speed.get(finalI) / (counter[0]));
                                    steps.set(finalI, steps.get(finalI) / (counter[0]));
                                    System.out.println( "average i =  "+finalI+" "+speed + "  "+ steps);
                                }
                            });
                        } else {
                            Log.d("TAG", "Document does not exist!");
                        }
                    } else {
                        Log.d("TAG", "Failed with: ", task.getException());
                    }
                }
            });
        }
    }

    private void generateCharts(){
        System.out.println("Hellooo");
        System.out.println( "average i =  "+speed + "  "+ steps);
        lv.setVisibility(View.VISIBLE);
        Tdata.setVisibility(View.VISIBLE);
        TextView TVdistance = findViewById(R.id.totaldistance);
        TextView TVduration = findViewById(R.id.totaltime);
        TVdistance.setText(String.valueOf((float)distance));
        TVduration.setText(String.valueOf((int)time/60));
        ArrayList<ChartItem> list = new ArrayList<>();
        list.add(new LineChartItem(generateDataLine(), getApplicationContext()));
        list.add(new BarChartItem(generateDataBar(), getApplicationContext()));
     //   list.add(new PieChartItem(generateDataPie(), getApplicationContext()));
        ChartDataAdapter cda = new ChartDataAdapter(getApplicationContext(), list);
        lv.setAdapter(cda);
    }

    /** adapter that supports 3 different item types */
    private class ChartDataAdapter extends ArrayAdapter<ChartItem> {

        ChartDataAdapter(Context context, List<ChartItem> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            //noinspection ConstantConditions
            return getItem(position).getView(position, convertView, getContext());
        }

        @Override
        public int getItemViewType(int position) {
            // return the views type
            ChartItem ci = getItem(position);
            return ci != null ? ci.getItemType() : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 3; // we have 3 different item-types
        }
    }

    /**
     * generates a random ChartData object with just one DataSet
     *
     * @return Bar data
     */
    private BarData generateDataBar() {

        ArrayList<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            entries.add(new BarEntry(i+1, (int) steps.get(i)));
        }

        BarDataSet d = new BarDataSet(entries, "Nombre des pas dans chaque entraînement");
        d.setColors(ColorTemplate.VORDIPLOM_COLORS);
        d.setHighLightAlpha(255);

        BarData cd = new BarData(d);
        cd.setBarWidth(0.9f);
        return cd;
    }

    /**
     * generates a random ChartData object with just one DataSet
     *
     * @return Line data
     */
    private LineData generateDataLine() {

        ArrayList<Entry> values1 = new ArrayList<>();

        for (int i = 0; i < speed.size(); i++) {
            values1.add(new Entry(i+1, (float) speed.get(i)));
        }

        LineDataSet d1 = new LineDataSet(values1, "Vitesses Moyennes de l'équipe (metre/seconde)");
        d1.setLineWidth(2.5f);
        d1.setCircleRadius(4.5f);
        d1.setHighLightColor(Color.rgb(244, 117, 117));
        d1.setColor(ColorTemplate.VORDIPLOM_COLORS[0]);
        d1.setCircleColor(ColorTemplate.MATERIAL_COLORS[0]);
        d1.setDrawValues(false);

        ArrayList<ILineDataSet> sets = new ArrayList<>();
        sets.add(d1);

        return new LineData(sets);
    }

    /**
     * generates a random ChartData object with just one DataSet
     *
     * @return Pie data
     */
    private PieData generateDataPie() {

        ArrayList<PieEntry> entries = new ArrayList<>();

        /*for (int i = 0; i < 2; i++) {
            entries.add(new PieEntry((float) ((Math.random() * 70) + 30), "Quarter " + (i+1)));
        }*/
        entries.add(new PieEntry((float) distance, "Distance(metre)" ));
        entries.add(new PieEntry((float) time/60, "Durée(min)" ));

        PieDataSet d = new PieDataSet(entries, " Distances et Durées Totales Parcourues par l'équipe");

        // space between slices
        d.setSliceSpace(2f);
        d.setColors(ColorTemplate.VORDIPLOM_COLORS);

        return new PieData(d);
    }


    public void BacktoDashboard(View v) {
        Intent intent = new Intent(CoachReportsActivity.this,CoachDashboardActivity.class);
        finish();
        startActivity(intent);
    }

}
