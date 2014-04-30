/* 
 * Copyright (C) 2014 TU Darmstadt, Hessen, Germany.
 * Department of Computer Science Databases and Distributed Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tudarmstadt.dvs.myhealthassistant.myhealthhub.fragments;

import android.support.v4.app.Fragment;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import de.tudarmstadt.dvs.myhealthassistant.myhealthhub.R;
import de.tudarmstadt.dvs.myhealthassistant.myhealthhub.graphActivities.GraphPlotBigActivity;
import de.tudarmstadt.dvs.myhealthassistant.myhealthhub.services.transformationmanager.database.LocalTransformationDBMS;
import de.tudarmstadt.dvs.myhealthassistant.myhealthhub.services.transformationmanager.database.TrafficData;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GraphPlotFragment extends Fragment {

	private static final String TAG = GraphPlotFragment.class.getSimpleName();
	private View rootView;

	private static boolean barType = true;

	private ArrayList<GraphViewData> data_light;
	private ArrayList<GraphViewData> data_acc;

	public static String lightGrpDes = "Light in lux/min";
	public static String motionGrpDes = "Motion Strength/min";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_with_graph, container,
				false);

		Log.e(TAG, ": onCreateView");

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);

		data_light = new ArrayList<GraphViewData>();
		data_acc = new ArrayList<GraphViewData>();

		CheckBox cBox = (CheckBox) rootView.findViewById(R.id.start_record);
		cBox.setVisibility(View.GONE);

		cBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// startRecording(isChecked);
				Log.e(TAG, "Recording sensors Evt = " + isChecked);
			}
		});

		Button refrBtn = (Button) rootView.findViewById(R.id.refresh_btn);
		refrBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearAllCharts();
			}
		});

		TextView atDate = (TextView) rootView.findViewById(R.id.at_date);
		atDate.setText(getCurrentDate());

		Button backBtn = (Button) rootView.findViewById(R.id.date_back);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dateBackAndForth(true);
			}
		});

		Button forthBtn = (Button) rootView.findViewById(R.id.date_next);
		forthBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dateBackAndForth(false);

			}
		});
		
		data_light = updateTrafficOnDate(getCurrentDate(), lightGrpDes);
		data_acc = updateTrafficOnDate(getCurrentDate(), motionGrpDes);
		redrawCharts();
		
		LinearLayout layout_light = (LinearLayout) rootView.findViewById(R.id.light_graph);
		LinearLayout layout_motion = (LinearLayout) rootView.findViewById(R.id.motion_graph);
		
		layout_light.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openBig();
			}
		});
		layout_motion.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openBig();
			}
		});
	}
	
	private void openBig(){
		//TODO
		Log.e(TAG, "todo");
		Intent i = new Intent(this.getActivity().getApplicationContext(), GraphPlotBigActivity.class);
		this.getActivity().startActivity(i);
		
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	private void dateBackAndForth(boolean back) {
		TextView atDate = (TextView) rootView.findViewById(R.id.at_date);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

		try {
			Date today = sdf.parse(atDate.getText().toString());
			Calendar cal = Calendar.getInstance();
			cal.setTime(today);

			if (back) {
				cal.add(Calendar.DATE, -1);
			} else {
				cal.add(Calendar.DATE, 1);
			}

			String newDate = sdf.format(cal.getTime());

			data_light = updateTrafficOnDate(newDate, lightGrpDes);
			data_acc = updateTrafficOnDate(newDate, motionGrpDes);
			redrawCharts();
			atDate.setText(newDate);

		} catch (ParseException e) {
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}

	}

	private LocalTransformationDBMS transformationDB;

	private ArrayList<GraphView.GraphViewData> updateTrafficOnDate(String date,
			String type) {
		// initialize database
		Log.e(TAG, "date=" + date + " type:" + type);
		this.transformationDB = new LocalTransformationDBMS(getActivity()
				.getApplicationContext());
		transformationDB.open();
		ArrayList<TrafficData> list = transformationDB.getAllTrafficFromDate(
				date, type);
		ArrayList<GraphView.GraphViewData> data = new ArrayList<GraphView.GraphViewData>();
		for (TrafficData t : list) {
			GraphViewData x = new GraphViewData(t.getxValue(), t.getyValue());
			data.add(x);
		}
		transformationDB.close();

		return data;
	}

	private void createGraph(String graphTitle, GraphViewSeries series,
			int Rid, boolean isBarChart) {
		GraphView graphView = null;

		if (isBarChart) {
			graphView = new BarGraphView(getActivity().getApplicationContext(),
					graphTitle);
			// graphView.setVerticalLabels(new String[] { "high", "mid", "low"
			// });
			graphView.setManualYAxisBounds(11.0d, 9.0d);
			// graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
			// @Override
			// public String formatLabel(double value, boolean isValueX) {
			// if (!isValueX) {
			// if (value <= 10) {
			// return "low";
			// } else if (10 < value && value < 11) {
			// return "mid";
			// } else {
			// return "hgh";
			// }
			// }
			// return null; // let graphview generate X-axis label for us
			// }
			// });
		} else
			graphView = new LineGraphView(
					getActivity().getApplicationContext(), graphTitle);

		graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
			@Override
			public String formatLabel(double value, boolean isValueX) {
				if (isValueX) {
					// convert (double) hour.mm to hour:mm
					return new DecimalFormat("00.00").format(value).replaceAll(
							"\\,", ":");
				}
				return null; // let graphview generate X-axis label for us
			}
		});

		// add data
		graphView.addSeries(series);
		// set view port, start=2, size=10
		// graphView.setViewPort(2, 10);
		graphView.setScrollable(false);
		// optional - activate scaling / zooming
		// graphView.setScalable(true);
		// optional - legend
		// graphView.setShowLegend(true);
		graphView.getGraphViewStyle().setNumVerticalLabels(3);
		graphView.getGraphViewStyle().setNumHorizontalLabels(5);

		// graphView.getGraphViewStyle().setNumVerticalLabels(7);
		// graphView.setManualYAxisBounds(300.0d, 0.0d);
		// graphView.setVerticalLabels(new String[] { "hgh", "mid", "low" });
		// graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
		// @Override
		// public String formatLabel(double value, boolean isValueX) {
		// if (!isValueX) {
		// if (value < 200) {
		// return "low";
		// } else if (value < 400) {
		// return "mid";
		// } else {
		// return "hgh";
		// }
		// }
		// return null; // let graphview generate X-axis label for us
		// }
		// });

		LinearLayout layout = (LinearLayout) rootView.findViewById(Rid);
		layout.removeAllViews();
		layout.addView(graphView);
		rootView.postInvalidate();
	}

	private void clearAllCharts() {
		clearChart(lightGrpDes);
		clearChart(motionGrpDes);
	}

	private void clearChart(String title) {
		GraphViewData[] dataList = new GraphViewData[1];
		dataList[0] = new GraphViewData(0.0, 0.0);
		GraphViewSeries gvs_series = new GraphViewSeries(dataList);
		
		if (title.equals(lightGrpDes)) {
			createGraph(lightGrpDes, gvs_series, R.id.light_graph, !barType);
			data_light = new ArrayList<GraphView.GraphViewData>();
		}
		if (title.equals(motionGrpDes)) {
			createGraph(motionGrpDes, gvs_series, R.id.motion_graph, barType);
			data_acc = new ArrayList<GraphView.GraphViewData>();
		}
	}

	private void redrawCharts() {
		if (data_light.size() > 0) {

			GraphViewData[] dataList = new GraphViewData[data_light.size()];
			for (int i = 0; i < data_light.size(); i++) {
				dataList[i] = data_light.get(i);
			}
			GraphViewSeries gvs_light = new GraphViewSeries(dataList);
			createGraph(lightGrpDes, gvs_light, R.id.light_graph, !barType);
		} else {
			clearChart(lightGrpDes);
		}
		if (data_acc.size() > 0) {
			GraphViewData[] dataAcc = new GraphViewData[data_acc.size()];
			for (int i = 0; i < data_acc.size(); i++) {
				dataAcc[i] = data_acc.get(i);
			}
			GraphViewSeries gvs_acc = new GraphViewSeries(dataAcc);
			createGraph(motionGrpDes, gvs_acc, R.id.motion_graph, barType);
		} else {
			clearChart(motionGrpDes);
		}
	}

	private static String getCurrentDate() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	@Override
	public void onStop() {
		super.onStop();
	}
}