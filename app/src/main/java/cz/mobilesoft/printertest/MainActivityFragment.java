package cz.mobilesoft.printertest;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.mobilesoft.printertest.util.PrintHelper;

public class MainActivityFragment extends Fragment implements PrintHelper.OnConnectedListener {
    @BindView(R.id.devicesRecyclerView)
    RecyclerView devicesRecyclerView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    private ProgressDialog dialog;
    private List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;

    private BroadcastReceiver broadcastReceiver = null;
    private IntentFilter intentFilter = null;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        ButterKnife.bind(this, view);
        dialog = new ProgressDialog(getContext());
        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter();
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        devicesRecyclerView.setAdapter(deviceAdapter);

        initBroadcastReceiver();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null)
            getActivity().unregisterReceiver(broadcastReceiver);
    }

    private void initBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    if (device == null)
                        return;
                    deviceList.add(device);
                    deviceAdapter.notifyDataSetChanged();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED
                        .equals(action)) {
                    progressBar.setIndeterminate(true);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                        .equals(action)) {
                    progressBar.setIndeterminate(false);
                }

            }

        };
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }

    @OnClick(R.id.searchButton)
    public void onClick() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            getActivity().finish();
            return;
        }

        if (!adapter.isEnabled() && !adapter.enable()) {
            getActivity().finish();
            return;
        }

        PrintHelper.get().disconnectBluetooth();
        adapter.cancelDiscovery();

        deviceList.clear();
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            deviceList.add(device);
        }
        deviceAdapter.notifyDataSetChanged();

        adapter.startDiscovery();
    }

    @Override
    public void onConnected(boolean isConnected) {
        dialog.hide();
        if (isConnected) {
            Toast.makeText(getContext(), "Success",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Fail",
                    Toast.LENGTH_SHORT).show();
        }
    }

    class DeviceAdapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.row_device, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BluetoothDevice device = deviceList.get(position);

            final String address = device.getAddress();
            String name = device.getName();
            if (name == null || name.equals(address))
                name = "Unknown device";

            holder.nameTextView.setText(name);
            holder.addressTextView.setText(address);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    PrintHelper.get().disconnectBluetooth();
                    // 只有没有连接且没有在用，这个才能改变状态!!! Jo, takže bacha!
                    dialog.setMessage("Connecting "
                            + address);
                    dialog.setIndeterminate(true);
                    dialog.setCancelable(false);
                    dialog.show();
                    PrintHelper.get().connectBluetooth(address, MainActivityFragment.this);
                }
            });
        }

        @Override
        public int getItemCount() {
            return deviceList.size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        TextView nameTextView;
        TextView addressTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            nameTextView = (TextView) itemView.findViewById(R.id.nameTextView);
            addressTextView = (TextView) itemView.findViewById(R.id.addressTextView);
        }
    }
}
