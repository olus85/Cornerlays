package app.olus.cornerlays;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/* loaded from: classes5.dex */
public class VolleySingleton {
    private static Context ctx;
    private static VolleySingleton instance;
    private RequestQueue requestQueue;

    private VolleySingleton(Context context) {
        ctx = context;
        this.requestQueue = getRequestQueue();
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        VolleySingleton volleySingleton;
        synchronized (VolleySingleton.class) {
            if (instance == null) {
                instance = new VolleySingleton(context);
            }
            volleySingleton = instance;
        }
        return volleySingleton;
    }

    public RequestQueue getRequestQueue() {
        if (this.requestQueue == null) {
            this.requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return this.requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
