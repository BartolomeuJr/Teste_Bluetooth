package com.example.teste_bluetooth;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    /*  Referências do Front  */
    private Button button;

    //    private BluetoothConfig bluetoothConfig;

    /*   Constantes   */
    private static final String UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB";
    private static final String DEVICE_NAME = "ECGNEO";

    /*  Objetos Bluetooth  */
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothSocket bluetoothSocket;
    private ActivityResultLauncher<Intent> bluetoothLauncher;


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        initializeBluetooth();


        //Evento do Botão
        button.setOnClickListener(v -> toggleButton());

    }

    private void initializeViews() {
        button = findViewById(R.id.button);
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Bluetooth was successfully enabled
                        Toast.makeText(MainActivity.this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                        getPairedDevices();
                    } else {
                        // User did not allow enabling Bluetooth
                        Toast.makeText(MainActivity.this, "Failed to enable Bluetooth", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void toggleButton() {
        if (!bluetoothAdapter.isEnabled()) {
            if (hasBluetoothPermissions()) {
                // Permissões já concedidas, ligar o Bluetooth
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                bluetoothLauncher.launch(enableBluetoothIntent);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_PERMISSION);

            }
        } else {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            }
            bluetoothAdapter.disable();
        }
    }

    private void getPairedDevices() {

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {}

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice pairedDevice : pairedDevices) {
                if (pairedDevice.getName().equals(DEVICE_NAME)) {
                    device = pairedDevice;
                    Log.d("Device", String.valueOf(device));
                    connectSocket();
                    break;
                }
            }
        } else {
            Toast.makeText(this, "Sem dispositivos emparelhados", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectSocket() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            Toast.makeText(this, "Conectando", Toast.LENGTH_SHORT).show();
            bluetoothAdapter.cancelDiscovery(); // Interrompe a descoberta de dispositivos Bluetooth

            // Use createInsecureRfcommSocketToServiceRecord() em vez de createRfcommSocketToServiceRecord()
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID_STRING));

            // Crie uma thread separada para conectar o socket Bluetooth
            Thread connectThread = new Thread(() -> {
                try {
                    if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}
                    bluetoothSocket.connect();

                    runOnUiThread(() -> {
                        if (bluetoothSocket.isConnected()) {
                            Toast.makeText(MainActivity.this, "Conectado", Toast.LENGTH_SHORT).show();
                            Log.d("Status: ", "Conectado ao disposivo" + device.getName() + "MAC: " + device.getAddress());
                        } else {
                            Toast.makeText(MainActivity.this, "Não foi possível conectar ao dispositivo", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao conectar ao dispositivo", Toast.LENGTH_SHORT).show());
                }
            });

            connectThread.start(); // Inicie a thread para conectar o socket Bluetooth
        } catch (IOException e) {
            Log.d("Exception", "Erro na conexão");
            Toast.makeText(this, "Erro ao conectar ao dispositivo", Toast.LENGTH_SHORT).show();
        }
    }



    private boolean hasBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permissions granted, enable Bluetooth
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                bluetoothLauncher.launch(enableBluetoothIntent);
            } else {
                Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
//    private void searchDevice() {
//
//        String deviceHardwareAddress;
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_PERMISSION);
//        }
//
//        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
//        for (BluetoothDevice device : devices) {
//            if (device.getName().equals("Pelicano")) {
//                this.device = device;
//                deviceHardwareAddress = device.getAddress();
//
//                Toast.makeText(this, this.device.toString(), Toast.LENGTH_SHORT).show();
//                Log.d("Econtrado", this.device.toString());
//                conectSocket();
//            }
////            Log.d( "END","Device Name: " + device.getName() + "\nDevice Address: " + device.getAddress());
//        }
//    }









































