# SpinedEditTextView
SpinedEditTextView

一个包含Spinner可选项的EditTextView

# 使用
1、Gradle
```
implementation 'com.wangyunxiu.lib:spedview_library:0.1'
```
2、InLayout
```
<com.wangyunxiu.lib.spinededview.SpinedEditView
        android:id="@+id/spinedEditView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
```
3、setItems OR  setAdapter    as you like
```
spinedEditView.setItems(listOf("ABC","ACB","CBA"))
```
  OR
```
spinedEditView.setListAdapter(...ListAdapter...)
```


![](https://github.com/15563988825/SpinedEditTextView/blob/master/art/1.png)
