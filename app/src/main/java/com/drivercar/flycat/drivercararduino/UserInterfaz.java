package com.drivercar.flycat.drivercararduino;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class UserInterfaz extends AppCompatActivity {


    Button forward_btn;
    Button reverse_btn;
    Button left_btn;
    Button right_btn;
    Button stop_btn;
    Button disconnect_btn;
    TextView bufferIn_tv;

    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    /** Identificador unico de servicio - Bluetooth HC-06 */
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = null;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_interfaz);

        forward_btn    = findViewById(R.id.forward_btn);
        reverse_btn    = findViewById(R.id.reverse_btn);
        left_btn       = findViewById(R.id.forward_left_btn);
        right_btn      = findViewById(R.id.forward_right_btn);
        stop_btn       = findViewById(R.id.stop_btn);
        disconnect_btn = findViewById(R.id.disconnect_btn);
        bufferIn_tv    = findViewById(R.id.bufferInTV);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIN.append(readMessage);

                    int endOfLineIndex = DataStringIN.indexOf("#");

                    if (endOfLineIndex > 0) {
                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        bufferIn_tv.setText("Dato: " + dataInPrint);
                        DataStringIN.delete(0, DataStringIN.length());
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        VerificarEstadoBT();

        /**
         * Implementa un listener que gestiona el click presionado en un boton
         * y la accion que debe realizar
         **/
        forward_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyConexionBT.write("a");
            }
        });
        reverse_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyConexionBT.write("e");
            }
        });
        left_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyConexionBT.write("b");
            }
        });
        right_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyConexionBT.write("d");
            }
        });
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyConexionBT.write("c");
            }
        });
        disconnect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btSocket != null) {
                    try {btSocket.close();
                    } catch (IOException e) {
                        Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();
                    }
                }
                finish();
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        /** Crea una instancia de Intent a partir del Intent enviado en la activity previa **/
        Intent intent = getIntent();
        /** Obtiene la direccion MAC enviada por el Intent e identificada por el TAG **/
        address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);
        /**
         * Setea la direccion MAC
         * Intenta crear y conectar el socket Bluetooth
         **/
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creación del Socket fallo", Toast.LENGTH_LONG).show();
        }
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {}
        }
        /** Crea un nuevo hilo e inicia la interacción con el modulo Blueetooth **/
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        /**
         * Si la aplicacion pasa a segundo plano, la comunicacion con el socket se cierra
         */
        try {
            btSocket.close();
        } catch (IOException e2) {}
    }

    /**
     * crea un conexion de salida segura para el dispositivo
     * usando el servicio UUID
     */
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private void VerificarEstadoBT() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    /** Clase que permite crear la conexion */
    public class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            /** Se mantiene en modo escucha para determinar el ingreso de datos */
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    /** Envia los datos obtenidos hacia el evento via handler */
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /**
         * Se envia por parametro la cadena obtenida en el evento click del boton
         * Envia los bytes al modulo Bluetooth
         */
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                /** Si no es posible enviar datos al modulo Bluetooth se cierra la conexión */
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}