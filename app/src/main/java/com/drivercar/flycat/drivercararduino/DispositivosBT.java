package com.drivercar.flycat.drivercararduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DispositivosBT extends AppCompatActivity {

    private static final String TAG = "DispositivosBT";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter mPairedDevicesArrayAdapter;

    ListView IdLista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos_bt);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        /** Funcion que verifica el estado(Encendido/Apagado) del Bluetooth **/
        VerificarEstadoBT();

        /** Inicializa el array y envia por parametro el recurso que contendra las instancias de la lista **/
        mPairedDevicesArrayAdapter = new ArrayAdapter(this, R.layout.nombre_dispositivos);

        IdLista = findViewById(R.id.listaLV);
        IdLista.setAdapter(mPairedDevicesArrayAdapter);
        IdLista.setOnItemClickListener(mDeviceClickListener);

        /** Asigna el adaptador local Bluetooth a la variable **/
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        /** Obtiene el conjunto de dispositos vinculadas actualmente **/
        Set <BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        /** Recorre los dispositivos vinculados y los agrega a la lista **/
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    /**
     * Adaptador que gestiona los clicks realizados por el usuario dentro de la lista
     * de dispostivos Bluetooth vinculados.
     */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView av, View v, int arg2, long arg3) {
            /** La dirección MAC del dispositivo, son los últimos 17 caracteres **/
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            /** Crea una instancia de Intent y envia por parametro la activity a inicializar **/
            Intent i = new Intent(DispositivosBT.this, UserInterfaz.class);
            /** Agrega un Tag al Intent que contiene la direccion MAC **/
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            /** Inicia la actividad colocada en el Intent **/
            startActivity(i);
        }
    };

    private void VerificarEstadoBT() {
        /** Asigna el adaptador local Bluetooth a la variable **/
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        /**
         * Comprueba que el adaptador no sea null y que este encendido.
         * Si el Bluetooth esta apagado; solicita al usuario activarlo.
         * Por medio de un intent manda un request al usuario esperando la aprobacion de la peticion
         */
        if(mBtAdapter == null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (mBtAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth Activado");
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
}