package mirglab.liza_alert_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import java.util.ArrayList;

public class ExpandableListAdapter extends BaseExpandableListAdapter implements Filterable {

    private LayoutInflater inflater;
    private ArrayList<ExpandableListParentClass<Object>> mParent;
    private View view;


    public ArrayList<ExpandableListParentClass<Object>> getMParent() {
        return mParent;
    }

    public ExpandableListAdapter(Context context, ArrayList<ExpandableListParentClass<Object>> parentList ) {
        this.mParent = parentList;
        this.inflater = LayoutInflater.from(context);

    }

    // counts the number of group/parent items so the list knows how many
    // times calls getGroupView() method
    public int getGroupCount() {
        return mParent.size();
    }

    // counts the number of children items so the list knows how many times
    // calls getChildView() method
    public int getChildrenCount(int parentPosition) {
        int size =0;
        if(mParent.get(parentPosition).getParentChildren() != null){
            size = mParent.get(parentPosition).getParentChildren().size();
        }
        return size;
    }

    // gets the title of each parent/group
    public Object getGroup(int i) {
        return mParent.get(i).getParent();
    }

    // gets the name of each item
    public Object getChild(int parentPosition, int childPosition) {
        return mParent.get(parentPosition).getParentChildren().get(childPosition);
    }

    public long getGroupId(int parentPosition) {
        return parentPosition;
    }

    public long getChildId(int i, int childPosition) {
        return childPosition;
    }

    public boolean hasStableIds() {
        return true;
    }

    // in this method you must set the text to see the parent/group on the list

    public View getGroupView(int parentPosition, boolean b, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.activity_groups, viewGroup, false);
        }
        return view;
    }

    // in this method you must set the text to see the children on the list

    public View getChildView(int parentPosition, int childPosition, boolean b, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.activity_groups, viewGroup, false);
        }

        // return the entire view
        return view;
    }

    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    @Override
    public Filter getFilter() {
        return null;
    }
}

