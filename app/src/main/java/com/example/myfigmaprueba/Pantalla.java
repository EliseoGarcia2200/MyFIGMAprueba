package com.example.myfigmaprueba;

import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

/** Componentes compartidos por los formularios y detalles, con el estilo de la aplicación. */
public final class Pantalla {
    private Pantalla() { }

    public static LinearLayout abrir(BaseActivity activity, String titulo) {
        activity.mostrarPantalla(R.layout.activity_contenido);
        ((TextView) activity.findViewById(R.id.tvTituloContenido)).setText(titulo);
        return activity.findViewById(R.id.contenido);
    }

    public static int dp(View v, int valor) { return Math.round(valor * v.getResources().getDisplayMetrics().density); }

    public static TextView texto(LinearLayout panel, String texto, boolean titulo) {
        TextView v = new TextView(panel.getContext());
        v.setText(texto); v.setTextSize(titulo ? 19 : 16);
        v.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.texto_oscuro));
        if (titulo) v.setTypeface(null, Typeface.BOLD);
        v.setTextIsSelectable(true);
        v.setPadding(0, dp(panel, 8), 0, dp(panel, 8)); panel.addView(v);
        return v;
    }

    public static Button boton(LinearLayout panel, String texto, View.OnClickListener accion) {
        AppCompatButton b = new AppCompatButton(panel.getContext());
        b.setText(texto); b.setTextSize(16); b.setAllCaps(false);
        b.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.blanco));
        b.setBackgroundResource(R.drawable.bg_boton_naranja);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(panel, 12); b.setMinHeight(dp(panel, 52)); b.setLayoutParams(lp);
        b.setPadding(dp(panel, 12), dp(panel, 8), dp(panel, 12), dp(panel, 8));
        b.setOnClickListener(accion); panel.addView(b); return b;
    }

    public static EditText campo(LinearLayout panel, String etiqueta, String valor, int tipo) {
        texto(panel, etiqueta, false);
        EditText e = new EditText(panel.getContext()); e.setTextSize(16);
        e.setInputType(tipo); e.setText(valor); e.setHint(etiqueta);
        e.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.texto_oscuro));
        e.setHintTextColor(ContextCompat.getColor(panel.getContext(), R.color.texto_secundario));
        e.setBackgroundResource(R.drawable.bg_input);
        e.setPadding(dp(panel, 12), dp(panel, 12), dp(panel, 12), dp(panel, 12));
        e.setMinimumHeight(dp(panel, 52)); panel.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    public static EditText campo(LinearLayout panel, String etiqueta, String valor) {
        return campo(panel, etiqueta, valor, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    }

    public static LinearLayout tarjeta(LinearLayout panel) {
        LinearLayout card = new LinearLayout(panel.getContext()); card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_input);
        int padding = dp(panel, 16); card.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(panel, 12);
        panel.addView(card, lp); return card;
    }
}
