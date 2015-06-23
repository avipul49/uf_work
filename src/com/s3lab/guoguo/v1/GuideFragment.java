package com.s3lab.guoguo.v1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GuideFragment extends Fragment {
    public GuideFragment() {
    }

    public static final String ARG_SECTION_NUMBER = "section_number";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    		View view = inflater.inflate(R.layout.fragment_guide, container,false);
//        TextView textView = new TextView(getActivity());
//        textView.setGravity(Gravity.CENTER);
//        Bundle args = getArguments();
//        textView.setText(Integer.toString(args.getInt(ARG_SECTION_NUMBER)));
        return view;
    }
}
