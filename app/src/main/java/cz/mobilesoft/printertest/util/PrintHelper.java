package cz.mobilesoft.printertest.util;

import android.os.AsyncTask;

import com.lvrenyang.io.BTPrinting;
import com.lvrenyang.io.Pos;

import java.io.UnsupportedEncodingException;

/**
 * Created by danielpichl on 19.08.16.
 */
public class PrintHelper {
    private static PrintHelper INSTANCE = new PrintHelper();
    private boolean isConnecting;
    private static BTPrinting bt = null;
    private static Pos pos = new Pos();

    private PrintHelper() {
    }

    public static PrintHelper get() {
        return INSTANCE;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public void connectBluetooth(String address, OnConnectedListener listener) {
        if (!isConnecting) new ConnectTask(address, listener).execute();
    }

    public void disconnectBluetooth() {
        if (bt != null) bt.Close();
    }

    public boolean print(byte[] data, int offset, int count) {
        return pos.IO.Write(data, offset, count) == count;
    }

    public boolean printTestPage() {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789\n";
        byte[] tmp1 = {0x1b, 0x40, (byte) 0xB2, (byte) 0xE2, (byte) 0xCA,
                (byte) 0xD4, (byte) 0xD2, (byte) 0xB3, 0x0A};
        byte[] tmp2 = {0x1b, 0x21, 0x01};
        byte[] tmp3 = {0x0A, 0x0A, 0x0A, 0x0A};
        byte[] buf = byteArraysToBytes(new byte[][]{tmp1,
                str.getBytes(), tmp2, str.getBytes(), tmp3});
//        return print(buf, 0, buf.length);
        String str1 = "ĚŠČŘŽÝÁÍÉŮÚěščřžýáíéůú";

        try {
            return print(str1.getBytes("CP437"), 0, str1.getBytes("CP437").length);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    public static byte[] byteArraysToBytes(byte[][] data) {
        int length = 0;
        for (byte[] aData : data) length += aData.length;
        byte[] send = new byte[length];
        int k = 0;
        for (byte[] aData : data)
            for (byte anAData : aData) send[k++] = anAData;
        return send;
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        String address;
        OnConnectedListener listener;

        public ConnectTask(String address, OnConnectedListener listener) {
            this.address = address;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            if (bt == null)
                bt = new BTPrinting();
            isConnecting = true;
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            pos.Set(bt);
            return bt.Open(address);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            isConnecting = false;
            listener.onConnected(result);
            super.onPostExecute(result);
        }
    }

    public interface OnConnectedListener {
        void onConnected(boolean isConnected);
    }
}
