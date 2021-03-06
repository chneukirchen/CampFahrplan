package nerd.tuxmobil.fahrplan.congress;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnLectureListClick}
 * interface.
 */
public class StarredListFragment extends AbstractListFragment implements AbsListView
        .MultiChoiceModeListener {

    private static final String LOG_TAG = "StarredListFragment";
    private OnLectureListClick mListener;
    private LectureList starredList;
    private boolean sidePane = false;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private LectureArrayAdapter mAdapter;

    public static StarredListFragment newInstance(boolean sidePane) {
        StarredListFragment fragment = new StarredListFragment();
        Bundle args = new Bundle();
        args.putBoolean(BundleKeys.SIDEPANE, sidePane);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StarredListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            sidePane = args.getBoolean(BundleKeys.SIDEPANE);
        }

        setHasOptionsMenu(true);
        starredList = FahrplanMisc.getStarredLectures(getActivity());

        mAdapter = new LectureArrayAdapter(getActivity(), starredList);
        MyApp.LogDebug(LOG_TAG, "onCreate, " + starredList.size() + " changes");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(),
                R.style.Theme_AppCompat_Light);

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        View view, header;
        if (sidePane) {
            view = localInflater.inflate(R.layout.fragment_favorites_list_narrow, container, false);
            mListView = (ListView) view.findViewById(android.R.id.list);
            header = localInflater.inflate(R.layout.starred_header, null, false);
        } else {
            view = localInflater.inflate(R.layout.fragment_favorites_list, container, false);
            mListView = (ListView) view.findViewById(android.R.id.list);
            header = localInflater.inflate(R.layout.header_empty, null, false);
        }
        mListView.addHeaderView(header);
        mListView.setHeaderDividersEnabled(false);

        // Set the adapter
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(this);

        return view;
    }

    private void jumpOverPastLectures() {
        Time now = new Time();
        now.setToNow();
        long nowMillis = now.toMillis(true);

        int i;
        int numSeparators = 0;
        for (i = 0; i < starredList.size(); i++) {
            Lecture lecture = starredList.get(i);
            if (lecture.dateUTC + (lecture.duration * 60000) > nowMillis) {
                numSeparators = lecture.day;
                break;
            }
        }
        if ((i > 0) && (i < starredList.size())) {
            mListView.setSelection(i+1+numSeparators);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        jumpOverPastLectures();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLectureListClick) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnLectureListClick");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onRefresh() {
        LectureList starred = FahrplanMisc.getStarredLectures(getActivity());
        starredList.clear();
        starredList.addAll(starred);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MyApp.LogDebug(LOG_TAG, "onItemClick");
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            position--;
            Lecture clicked = starredList.get(mAdapter.getMapper().get(position));
            mListener.onLectureListClick(clicked);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.starred_list_menu, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_clear_all:
                int count = starredList.size();
                for (int i = 0; i < count; i++) {
                    deleteItem(0);
                }
                refreshViews();
                return true;
            case android.R.id.home:
                return ActivityHelper.navigateUp(getActivity());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.starred_list_context_menu, menu);
        mode.setTitle(getString(R.string.choose_to_delete));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_delete:
                deleteItems(mListView.getCheckedItemPositions());
                refreshViews();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private void deleteItem(int index) {
        Lecture l = starredList.get(index);
        l.highlight = false;
        FahrplanMisc.writeHighlight(getActivity(), l);
        if (MyApp.lectureList != null) {
            for (int j = 0; j < MyApp.lectureList.size(); j++) {
                Lecture lecture = MyApp.lectureList.get(j);
                if (lecture.lecture_id.equals(l.lecture_id)) {
                    lecture.highlight = false;
                    break;
                }
            }
        }
        starredList.remove(index);
    }

    private void deleteItems(SparseBooleanArray checkedItemPositions) {
        for (int id = mListView.getAdapter().getCount()-1; id >= 0; id--) {
            if (checkedItemPositions.get(id)) {
                deleteItem(mAdapter.getMapper().get(id-1));
            }
        }
    }

    private void refreshViews() {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).refreshEventMarkers();
        }
        mAdapter.notifyDataSetChanged();
        getActivity().setResult(FragmentActivity.RESULT_OK);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }
}
