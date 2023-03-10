package com.example.vpnguidetest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;


public class ToyVpnClient extends AppCompatActivity {

    public interface Prefs {
        String NAME = "connection";
        String SERVER_ADDRESS = "server.address";
        String SERVER_PORT = "server.port";
        String SHARED_SECRET = "shared.secret";
        String PROXY_HOSTNAME = "proxyhost";
        String PROXY_PORT = "proxyport";
        String ALLOW = "allow";
        String PACKAGES = "packages";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form);

        Log.d("Mytag", "Start layout");

        final TextView serverAddress = findViewById(R.id.address);
        final TextView serverPort = findViewById(R.id.port);
        final TextView sharedSecret = findViewById(R.id.secret);
        final TextView proxyHost = findViewById(R.id.proxyhost);
        final TextView proxyPort = findViewById(R.id.proxyport);
        final RadioButton allowed = findViewById(R.id.allowed);
        final TextView packages = findViewById(R.id.packages);

        final SharedPreferences prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);

        serverAddress.setText(prefs.getString(Prefs.SERVER_ADDRESS, ""));

        int serverPortPrefValue = prefs.getInt(Prefs.SERVER_PORT, 0);
        serverPort.setText(String.valueOf(serverPortPrefValue == 0 ? "" : serverPortPrefValue));
        sharedSecret.setText(prefs.getString(Prefs.SHARED_SECRET, ""));
        proxyHost.setText(prefs.getString(Prefs.PROXY_HOSTNAME, ""));

        int proxyPortPrefValue = prefs.getInt(Prefs.PROXY_PORT, 0);
        proxyPort.setText(proxyPortPrefValue == 0 ? "" : String.valueOf(proxyPortPrefValue));
        allowed.setChecked(prefs.getBoolean(Prefs.ALLOW, true));

        packages.setText(String.join(", ", prefs.getStringSet(
                Prefs.PACKAGES, Collections.emptySet())));


        findViewById(R.id.connect).setOnClickListener(v -> {
            if (!checkProxyConfigs(proxyHost.getText().toString(),
                    proxyPort.getText().toString())) {
                return;
            }
            final Set<String> packageSet =
                    Arrays.stream(packages.getText().toString().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());

            if (!checkPackages(packageSet)) {
                return;
            }
            int serverPortNum;
            try {
                serverPortNum = Integer.parseInt(serverPort.getText().toString());
            } catch (NumberFormatException e) {
                serverPortNum = 0;
            }
            int proxyPortNum;
            try {
                proxyPortNum = Integer.parseInt(proxyPort.getText().toString());
            } catch (NumberFormatException e) {
                proxyPortNum = 0;
            }
            prefs.edit()
                    .putString(Prefs.SERVER_ADDRESS, serverAddress.getText().toString())
                    .putInt(Prefs.SERVER_PORT, serverPortNum)
                    .putString(Prefs.SHARED_SECRET, sharedSecret.getText().toString())
                    .putString(Prefs.PROXY_HOSTNAME, proxyHost.getText().toString())
                    .putInt(Prefs.PROXY_PORT, proxyPortNum)
                    .putBoolean(Prefs.ALLOW, allowed.isChecked())
                    .putStringSet(Prefs.PACKAGES, packageSet)
                    .commit();

            // Only one app can be the current prepared VPN service. Always call VpnService.prepare()
            // because a person might have set a different app as the VPN service since your app last called the method.

            // Prepare VPN Service for connection
            Intent intent = VpnService.prepare(ToyVpnClient.this);
            if (intent != null) {

                // Show dialog to ask for connection
                startActivityForResult(intent, 0);
            }
            // if rights already have
            else {
                onActivityResult(0, RESULT_OK, null);
            }
        });

        findViewById(R.id.disconnect).setOnClickListener(v -> {
            startService(getServiceIntent().setAction(ToyVpnService.ACTION_DISCONNECT));
        });
    }

    private boolean checkProxyConfigs(String proxyHost, String proxyPort) {
        final boolean hasIncompleteProxyConfigs = proxyHost.isEmpty() != proxyPort.isEmpty();
        if (hasIncompleteProxyConfigs) {
            Toast.makeText(this, R.string.incomplete_proxy_settings, Toast.LENGTH_SHORT).show();
        }
        return !hasIncompleteProxyConfigs;
    }
    private boolean checkPackages(Set<String> packageNames) {
        final boolean hasCorrectPackageNames = packageNames.isEmpty() ||
                getPackageManager().getInstalledPackages(0).stream()
                        .map(pi -> pi.packageName)
                        .collect(Collectors.toSet())
                        .containsAll(packageNames);
        if (!hasCorrectPackageNames) {
            Toast.makeText(this, R.string.unknown_package_names, Toast.LENGTH_SHORT).show();
        }
        return hasCorrectPackageNames;
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
//        Add super call
        super.onActivityResult(request, result, data);
        if (result == RESULT_OK) {
            startService(getServiceIntent().setAction(ToyVpnService.ACTION_CONNECT));
        }
    }
    private Intent getServiceIntent() {
        return new Intent(this, ToyVpnService.class);
    }

}