# RotateListView
可以旋转的listview

## 说明
该组件是模仿宝宝生育树app的圆形listview，数据滑到最后一个就不能再滑动，可以点击选择、快速滑动。
滑动结束回调显示

## 基本功能
 
 * 可以以列表的形式加载数据
 * 第一个数据和最后一个数据不能再向前和向后滑动
 * 可以进行自定义布局显示
 * 选择某一个数据显示

## 事件监听
 * OnItemClickListener
 * OnItemSelectedListener
 * OnRotationFinishedListener


## 应用截图

  ![] (./resource/Screenshot_1.png)
  ![] (./resource/Screenshot_2.png)

## 使用demo

```
    xml
        <RelativeLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content">

            <ImageView
                android:id="@+id/rotate_listview_bg"
                android:layout_width="match_parent"
                android:layout_height="400dp"
                android:layout_centerInParent="true"
                android:src="@mipmap/semicircle_circle" />

            <github.liusp.rotatelistview.RotateListView
                android:id="@+id/rotate_listview"
                android:layout_width="match_parent"
                android:layout_height="400dp"
                android:layout_centerInParent="true" />
        </RelativeLayout>

   java
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

```
## Thanks

  * https://github.com/szugyi/Android-CircleMenu

  * https://github.com/JakeWharton/NineOldAndroids

## License

Copyright (c) 2016 QDPeng

Licensed under the [Apache License, Version 3.0](http://opensource.org/licenses/GPL-3.0)

