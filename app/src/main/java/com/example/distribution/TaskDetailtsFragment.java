package com.example.distribution;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class TaskDetailtsFragment extends Fragment {

    public interface OnFragmentSendDetailsToEdit{
        void onSendDetailsToEdit(Distribution distribution);
        void onCloseTaskDetailsFragment();
    }

    private OnFragmentSendDetailsToEdit fragmentSendDetailsToEdit;

    private Distribution distribution;
    private TextView textTaskName, textTaskDescription, textTaskExpDate, textTaskExpTime, textTaskWorker;
    private Button buttonTaskSeen, buttonTaskCompleted, buttonEditTask, buttonCloseTask;

    private static final String PREFS_FILE = "Account";
    private static final String PREF_ROLE = "Worker";
    private String userRole;

    private DatabaseReference databaseReference, databaseReferenceTracking;
    private String DISTRIBUTION_KEY = "Distribution", TRACKING_KEY = "TaskTracking";

    public TaskDetailtsFragment(Distribution distribution) {
        this.distribution = distribution;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            fragmentSendDetailsToEdit = (OnFragmentSendDetailsToEdit) context;
        }
        catch (ClassCastException e){
            Toast.makeText(getActivity(), "Interface error", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getUserRole();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_detailts, container, false);

        textTaskName = view.findViewById(R.id.textTaskName);
        textTaskDescription = view.findViewById(R.id.textTaskDescription);
        textTaskExpDate = view.findViewById(R.id.textTaskExpDate);
        textTaskExpTime = view.findViewById(R.id.textTaskExpTime);
        textTaskWorker = view.findViewById(R.id.textTaskWorker);

        textTaskName.setText(distribution.getTaskName());
        textTaskDescription.setText(distribution.getTaskDescription());
        textTaskExpDate.setText(distribution.getTaskExpirationDate().substring(3));
        textTaskExpTime.setText(distribution.getTaskExpirationTime());
        textTaskWorker.setText(distribution.getTaskWorker());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        buttonTaskSeen = view.findViewById(R.id.buttonTaskSeen);
        buttonTaskCompleted = view.findViewById(R.id.buttonTaskCompleted);
        buttonEditTask = view.findViewById(R.id.buttonEditTask);
        buttonCloseTask = view.findViewById(R.id.buttonCloseTask);

        databaseReference = FirebaseDatabase.getInstance().getReference(DISTRIBUTION_KEY);
        databaseReferenceTracking = FirebaseDatabase.getInstance().getReference(TRACKING_KEY).child("Tracking");

        if (!userRole.equals("Worker")){
            buttonTaskSeen.setVisibility(View.GONE);
            buttonTaskCompleted.setVisibility(View.GONE);
            buttonEditTask.setVisibility(View.VISIBLE);
            buttonCloseTask.setVisibility(View.VISIBLE);
        }

        buttonEditTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentSendDetailsToEdit.onSendDetailsToEdit(distribution);
            }
        });

        buttonCloseTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeTask("Задача удалена!");
                editTasksCount("Closed");
            }
        });

        buttonTaskCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeTask("Задача отмечена как выполенная");
                editTasksCount("Completed");
            }
        });

        buttonTaskSeen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Задача отмечена как просмотренная");
                editTasksCount("Seen");
                fragmentSendDetailsToEdit.onCloseTaskDetailsFragment();
            }
        });
    }

    private void getUserRole(){
        userRole = getActivity().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).getString(PREF_ROLE, "Worker");
    }

    private void removeTask(String toastToShow){
        databaseReference.child(distribution.getTaskName()).removeValue();
        showToast(toastToShow);
        fragmentSendDetailsToEdit.onCloseTaskDetailsFragment();
    }

    private void showToast(String text){
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    private void editTasksCount(String type){
        databaseReferenceTracking.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer currentValueSeen = currentData.child("seen").getValue(Integer.class);
                Integer currentValueIssued = currentData.child("issued").getValue(Integer.class);
                Integer currentValueCompleted = currentData.child("completed").getValue(Integer.class);

                if (currentValueSeen == null) { currentData.child("seen").setValue(0); }
                if (currentValueIssued == null) { currentData.child("issued").setValue(0); }
                if (currentValueCompleted == null) { currentData.child("completed").setValue(0); }

                else{
                    if (type.equals("Seen")){
                        currentData.child("seen").setValue(currentValueSeen + 1);
                        currentData.child("issued").setValue(currentValueIssued - 1);
                    }
                    if (type.equals("Closed")){
                        currentData.child("issued").setValue(currentValueIssued - 1);
                    }
                    if (type.equals("Completed")){
                        currentData.child("seen").setValue(currentValueSeen - 1);
                        currentData.child("completed").setValue(currentValueCompleted + 1);
                    }
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

            }
        });
    }
}