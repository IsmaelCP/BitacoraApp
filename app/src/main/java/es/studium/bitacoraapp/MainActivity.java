package es.studium.bitacoraapp;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
{
    ListView listaCuadernos;
    ArrayList<String> cuadernos;
    String idCuaderno, nombreCuaderno;

    ConsultaRemota acceso;
    AltaRemota alta;
    BajaRemota baja;
    JSONArray result;
    JSONObject jsonobject;
    int posicion;
    ArrayAdapter<String> adapter;

    FloatingActionButton fabCuadernos;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        listaCuadernos = findViewById(R.id.listaCuadernos);
        fabCuadernos = findViewById(R.id.fabCuadernos);

        cuadernos = new ArrayList<>();

        // Creamos el adaptador
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cuadernos);

        // Asignamos el adaptador a nuestro ListView
        listaCuadernos.setAdapter(adapter);

        acceso = new ConsultaRemota();
        acceso.execute();

        // Asignamos un listener a cada elemento de la lista
        listaCuadernos.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent visorDetalles = new Intent(view.getContext(), Apuntes.class);
                visorDetalles.putExtra("cuaderno", cuadernos.get(position));
                startActivity(visorDetalles);
            }
        });

        // Realizamos el alta de un cuaderno desde el bot??n fabCuadernos
        fabCuadernos.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Cuaderno Nuevo");
                alertDialog.setMessage("Introduce el nombre");
                final EditText nombreCuaderno = new EditText(MainActivity.this);
                nombreCuaderno.setHint("Nombre");
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                nombreCuaderno.setLayoutParams(layoutParams);
                alertDialog.setView(nombreCuaderno);
                alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                alta = new AltaRemota(nombreCuaderno.getText().toString());
                                alta.execute();
                                acceso = new ConsultaRemota();
                                acceso.execute();
                            }
                        });
                alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
            }
        });

        // Realizamos la baja de un cuaderno con una pulsaci??n larga en el cuaderno
        listaCuadernos.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> arg0, View v, int index, long arg3)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Confirma si quieres eliminar el cuaderno")
                        .setCancelable(false)
                        .setPositiveButton("Eliminar", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                // Identifico el id del cuaderno seleccionado
                                String[] cadena = listaCuadernos.getItemAtPosition(index).toString().split("  -  ");
                                baja = new BajaRemota(cadena[0]);
                                baja.execute();
                                acceso = new ConsultaRemota();
                                acceso.execute();
                            }
                        })
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });
    }

    // Consulta Cuadernos
    private class ConsultaRemota extends AsyncTask<Void, Void, String>
    {
        // Constructor
        public ConsultaRemota() {}
        // Inspectores
        protected void onPreExecute()
        {
            Toast.makeText(MainActivity.this, "Obteniendo datos...", Toast.LENGTH_SHORT).show();
        }
        protected String doInBackground(Void... argumentos)
        {
            try
            {
                // Crear la URL de conexi??n al API
                URL url = new URL("http://192.168.1.187/ApiRest/cuadernos.php");
                // Crear la conexi??n HTTP
                HttpURLConnection myConnection = (HttpURLConnection) url.openConnection();
                // Establecer m??todo de comunicaci??n. Por defecto GET.
                myConnection.setRequestMethod("GET");
                if (myConnection.getResponseCode() == 200)
                {
                    // Conexi??n exitosa
                    // Creamos Stream para la lectura de datos desde el servidor
                    InputStream responseBody = myConnection.getInputStream();
                    InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                    // Creamos Buffer de lectura
                    BufferedReader bR = new BufferedReader(responseBodyReader);
                    String line = "";
                    StringBuilder responseStrBuilder = new StringBuilder();
                    // Leemos el flujo de entrada
                    while ((line = bR.readLine()) != null)
                    {
                        responseStrBuilder.append(line);
                    }
                    // Parseamos respuesta en formato JSON
                    result = new JSONArray(responseStrBuilder.toString());
                    // Nos quedamos solamente con la primera
                    posicion = 0;
                    jsonobject = result.getJSONObject(posicion);
                    // Sacamos dato a dato obtenido
                    idCuaderno = jsonobject.getString("idCuaderno");
                    nombreCuaderno = jsonobject.getString("nombreCuaderno");
                    responseBody.close();
                    responseBodyReader.close();
                    myConnection.disconnect();
                }
                else
                {
                    // Error en la conexi??n
                    Log.println(Log.ERROR, "Error", "??Conexi??n fallida!");
                }
            }
            catch (Exception e)
            {
                Log.println(Log.ERROR, "Error", "??Conexi??n fallida");
            }
            return (null);
        }

        protected void onPostExecute(String mensaje)
        {
            // A??ado los cuadernos obtenidos a la lista
            try
            {
                cuadernos.clear();
                if (result != null)
                {
                    int longitud = result.length();
                    for (int i = 0; i < longitud; i++)
                    {
                        jsonobject = result.getJSONObject(i);
                        cuadernos.add(jsonobject.getString("idCuaderno") + "  -  " + jsonobject.getString("nombreCuaderno"));
                        adapter.notifyDataSetChanged();
                    }
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    // Alta Cuadernos
    private class AltaRemota extends AsyncTask<Void, Void, String>
    {
        // Atributos
        String nombreCuaderno;
        // Constructor
        public AltaRemota (String nombre) {this.nombreCuaderno = nombre;}
        // Inspectores
        protected void onPreExecute(){}
        protected String doInBackground(Void... argumentos)
        {
            try
            {
                // Crear la URL de conexi??n al API
                URL url = new URL("http://192.168.1.187/ApiRest/cuadernos.php");
                // Crear la conexi??n HTTP
                HttpURLConnection myConnection = (HttpURLConnection) url.openConnection();
                // Establecer m??todo de comunicaci??n
                myConnection.setRequestMethod("POST");
                // Conexi??n exitosa
                HashMap<String, String> postDataParams = new HashMap<>();
                postDataParams.put("nombreCuaderno", this.nombreCuaderno);
                myConnection.setDoInput(true);
                myConnection.setDoOutput(true);
                OutputStream os = myConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));
                writer.flush();
                writer.close();
                os.close();
                myConnection.getResponseCode();
                if(myConnection.getResponseCode() == 200)
                {
                    // Success
                    myConnection.disconnect();
                }
                else{
                    // Error handling code goes here
                    Log.println(Log.ASSERT, "Error", "Error");
                }
            }
            catch (Exception e)
            {
                Log.println(Log.ASSERT, "Excepci??n", e.getMessage());
            }
            return (null);
        }
        protected void onPostExecute(String mensaje){}
        private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException
        {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for(Map.Entry<String, String> entry : params.entrySet())
            {
                if(first)
                {
                    first = false;
                }
                else{
                    result.append("&");
                }
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return result.toString();
        }
    }

    private class BajaRemota extends AsyncTask<Void, Void, String>
    {
        // Atributos
        String idCuaderno;
        // Constructor
        public BajaRemota(String id){this.idCuaderno = id;}
        // Inspectores
        protected void onPreExecute() {}
        @Override
        protected String doInBackground(Void... voids)
        {
            // Comprobamos que el elemento cuaderno est?? vac??o
            try
            {
                // Creamos la URL de conexi??n al API
                Uri uri = new Uri.Builder().scheme("http").authority("192.168.1.187").path("/ApiRest/apuntes.php").appendQueryParameter("idCuaderno", this.idCuaderno).build();
                URL url = new URL(uri.toString());
                // Create connection
                HttpURLConnection myConnection = (HttpURLConnection) url.openConnection();
                // Establecemos m??todo. Por defecto GET.
                myConnection.setRequestMethod("GET");
                if(myConnection.getResponseCode() == 200)
                {
                    InputStream responseBody = myConnection.getInputStream();
                    InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                    BufferedReader bR = new BufferedReader(responseBodyReader);
                    String line;
                    StringBuilder responseStrBuilder = new StringBuilder();
                    // Leemos el flujo de entrada
                    while ((line = bR.readLine()) != null)
                    {
                        responseStrBuilder.append(line);
                    }
                    // Parseamos respuesta en formato JSON
                    result = new JSONArray(responseStrBuilder.toString());
                    responseBody.close();
                    responseBodyReader.close();
                    myConnection.disconnect();
                }
                else
                {
                    // Error en la conexi??n
                    Log.println(Log.ERROR, "Error", "??Conexi??n fallida!");
                }
            }
            catch (Exception e)
            {
                Log.println(Log.ERROR, "Error", "??Conexi??n fallida!");
            }
            finally
            {
                // Borramos el cuaderno si est?? vac??o o mostramos un mensaje de error si el cuaderno no est?? vac??o
                if(result.length() == 0)
                {
                    try
                    {
                        // Crear la URL de conexi??n al API
                        URI baseUri = new URI("http://192.168.1.187/ApiRest/cuadernos.php");
                        String[] parametros = {"id", this.idCuaderno};
                        URI uri = applyParameters(baseUri, parametros);
                        // Create connection
                        HttpURLConnection myConnection = (HttpURLConnection) uri.toURL().openConnection();
                        // Establecer m??todo. Por defecto GET
                        myConnection.setRequestMethod("DELETE");
                        if (myConnection.getResponseCode() == 200)
                        {
                            // Success
                            Log.println(Log.ASSERT, "Resultado", "Cuaderno eliminado");
                            myConnection.disconnect();
                        }
                        else
                        {
                            // Error handling code goes here
                            Log.println(Log.ASSERT, "Error", "Error");
                        }
                    }
                    catch (Exception e)
                    {
                        Log.println(Log.ASSERT, "Excepci??n", e.getMessage());
                    }
                }
                else
                {
                    // Mostramos un mensaje indicando que NO se puede eliminar un cuaderno con registros
                    runOnUiThread(new Runnable() {
                        public void run() {
                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                            alertDialog.setTitle("Alerta");
                            alertDialog.setMessage("No puedes eliminar un cuaderno con registros, elimine primero los apuntes");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Aceptar", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                    });
                }
            }
            return null;
        }
        protected void onPostExecute(String mensaje) {}
        URI applyParameters(URI uri, String[] urlParameters)
        {
            StringBuilder query = new StringBuilder();
            boolean first = true;
            for(int i = 0; i < urlParameters.length; i+= 2)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    query.append("&");
                }
                try
                {
                    query.append(urlParameters[i]).append("=").append(URLEncoder.encode(urlParameters[i + 1], "UTF-8"));
                }
                catch (UnsupportedEncodingException ex)
                {
                    // As URLEncoder are always correct, this exception should never be thrown.
                    throw new RuntimeException(ex);
                }
            }
            try
            {
                return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query.toString(), null);
            }
            catch (Exception ex)
            {
                // As URLEncoder are always correct, this exception should never be thrown.
                throw new RuntimeException(ex);
            }
        }
    }
}