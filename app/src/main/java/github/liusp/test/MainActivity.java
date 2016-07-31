package github.liusp.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

import github.liusp.rotatelistview.ItemEntity;
import github.liusp.rotatelistview.RotateListView;

public class MainActivity extends AppCompatActivity {
    private RotateListView rotateListView;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        if (toolbar != null) {
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
            toolbar.setTitle(getString(R.string.app_name));
            setSupportActionBar(toolbar);
        }
        initView();
    }

    private void initView() {
        imageView = (ImageView) findViewById(R.id.rotate_listview_bg);
        rotateListView = (RotateListView) findViewById(R.id.rotate_listview);
        rotateListView.setOnRotationFinishedListener(new RotateListView.OnRotationFinishedListener() {
            @Override
            public void onRotationFinished(RotateListView.ItemView view) {
                Toast.makeText(MainActivity.this, "the id is:" + view.getIncreaseId(), Toast.LENGTH_SHORT).show();
            }
        });
        initDisplay();
        if (rotateListView != null)
            rotateListView.setCircleBg(imageView);
        initData();
    }

    private void initDisplay() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int w = dm.widthPixels;
        ViewGroup.LayoutParams rotateLP = rotateListView.getLayoutParams();
        rotateLP.height = w;
        rotateListView.setLayoutParams(rotateLP);

        ViewGroup.LayoutParams bgLp = imageView.getLayoutParams();
        bgLp.height = w;
        imageView.setLayoutParams(bgLp);
    }

    private void initData() {
        ArrayList<ItemEntity> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ItemEntity entity = new ItemEntity();
            entity.setName("Data" + i);
            entity.setSubName("SubData" + i);
            list.add(entity);
        }
        rotateListView.setEntitys(list);
        rotateListView.setChildAngles();
    }

    public void setPosition(View view) {
        rotateListView.setTodayIndex(5);
        rotateListView.setChildLayout(5);
    }

}
