package io.mob.resu.reandroidsdk;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.mob.resu.reandroidsdk.error.ExceptionTracker;
import io.mob.resu.reandroidsdk.error.Log;


class DataNetworkHandler implements IResponseListener {

    private Context context;
    private ArrayList<MData> dbScreen;
    private ArrayList<MData> dbCampaign;


    public static DataNetworkHandler getInstance() {
        return new DataNetworkHandler();
    }


    void onMakeTrackingRequest(Context context) {
        this.context = context;
        new DataGetting().execute();
    }


    void campaignHandler(Context context, String id, String status, boolean isNewInstall, String rating, String comments) {
        this.context = context;

        //CampaignTracking campaignTracking = null;

        try {
            // local database entry
            JSONObject jsonObject = getDatabaseObject(context, isNewInstall, id, status, rating, comments);
            dataBaseCampaignViewAddEntry(context, jsonObject);

            // get From Local DataBase
            // campaignTracking = getCampaignDataFromLocalDataBase(context);
            String campaignTracking = getCampaignFromLocalDataBase(context);
            if (Util.getInstance(context).hasNetworkConnection()) {
                apiCallCampaignTracking(campaignTracking, id);
            }


        } catch (Exception e) {
            ExceptionTracker.track(e);
        }
    }


    private void apiCallScreenTracking(String screenTracking) {
        new DataExchanger("screenTracking", screenTracking, this, AppConstants.SDK_SCREEN_TACKING).execute();
    }

    void onUpdateLocation(JSONObject jsonObject) {
        new DataExchanger("locationUpdate", jsonObject.toString(), this, AppConstants.SDK_LOCATION_TACKING).execute();

    }

    private void apiCallCampaignTracking(String campaignTracking, String id) {

        new DataExchanger("https://b.resu.io/GCM/GetSmartCodeDetail?smartCode=" + id, "", this, AppConstants.SDK_CAMPAIGN_DETAILS).execute();

        new DataExchanger("campaignTracking", campaignTracking, this, AppConstants.SDK_NOTIFICATION_VIEWED).execute();


    }


    @NonNull
    private JSONObject getDatabaseObject(Context context, boolean isNewInstall, String id, String status, String rating, String comments) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(context.getString(R.string.resulticksApiParamId), id);
        jsonObject.put(context.getString(R.string.resulticksApiParamIsNewUser), isNewInstall);
        jsonObject.put(context.getString(R.string.resulticksApiParamStatus), status);

        if (rating != null)
            jsonObject.put(context.getString(R.string.resulticksApiParamRating), rating);
        if (comments != null)
            jsonObject.put(context.getString(R.string.resulticksApiParamComments), comments);

        jsonObject.put(context.getString(R.string.resulticksApiParamTimeStamp), Util.getCurrentUTC());
        return jsonObject;
    }


    @NonNull
    private String getCampaignFromLocalDataBase(Context context) {
        JSONObject campaignObj = new JSONObject();
        try {
            dbCampaign = new DataBase(context).getData(DataBase.Table.CAMPAIGN_TABLE);

            campaignObj.put("appId", SharedPref.getInstance().getStringValue(context, context.getString(R.string.resulticksSharedAPIKey)));

            ArrayList<JSONObject> campaigns = new ArrayList<>();

            // making format of webservice
            if (dbCampaign != null && dbCampaign.size() > 0) {
                for (MData mData : dbCampaign) {
                    String s = mData.getValues();
                    JSONObject jsonObject1 = new JSONObject(s);
                    campaigns.add(jsonObject1);
                }
            }
            campaignObj.put("campaigns", new JSONArray(campaigns));


        }catch (Exception e)
        {
           ExceptionTracker.track(e);
        }
        return campaignObj.toString();
    }


    /**
     * Database entry
     */
    private void dataBaseCampaignViewAddEntry(Context context, final JSONObject jsonObject) {

        try {
            new DataBase(context).insertData(jsonObject.toString(), DataBase.Table.CAMPAIGN_TABLE);
        } catch (Exception e) {
            ExceptionTracker.track(e);
        }

    }


    @Override
    public void onSuccess(String response, int flag) {
        apiResponseHandler(response, flag);

    }

    @Override
    public void onFailure(Throwable throwable, int flag) {
        apiResponseHandler("", flag);
    }

    @Override
    public void showDialog(String response, int flag) {
        apiResponseHandler("", flag);
    }


    @Override
    public void showErrorDialog(String errorResponse, int flag) {
        apiResponseHandler("", flag);
    }

    @Override
    public void showInternalServerErrorDialog(String errorResponse, int flag) {
        apiResponseHandler("", flag);
    }

    @Override
    public void logOut(int flag) {

    }


    /**
     * API Server Communication
     */

    private void apiResponseHandler(String response, int flag) {
        try {
            switch (flag) {
                case AppConstants.SDK_NOTIFICATION_VIEWED:
                    try {
                        new DataBase(context).deleteData(dbCampaign, DataBase.Table.CAMPAIGN_TABLE);
                    } catch (Exception e) {
                        ExceptionTracker.track(e);
                    }
                    break;

                case AppConstants.SDK_SCREEN_TACKING:

                    Log.e("onScreenTracked", response);
                    try {
                        new DataBase(context).deleteData(dbScreen, DataBase.Table.SCREENS_TABLE);
                    } catch (Exception e) {
                        ExceptionTracker.track(e);
                    }
                    break;


                case AppConstants.SDK_CAMPAIGN_DETAILS:
                    SharedPref.getInstance().setSharedValue(context, context.getString(R.string.resulticksSharedReferral), response);
                    break;

            }
        } catch (Exception e) {
            ExceptionTracker.track(e);
        }
    }

    private class DataGetting extends AsyncTask<String, String, String> {


        @Override
        protected String doInBackground(String... params) {


            JSONObject jsonObject = null;

            try {
                if (Util.getInstance(context).hasNetworkConnection()) {

                    dbScreen = new DataBase(context).getData(DataBase.Table.SCREENS_TABLE);

                    ArrayList<JSONObject> screenArrayList = new ArrayList<>();
                    // JSONObject jsonObject1 = new JSONObject();
                    if (dbScreen != null && dbScreen.size() > 0) {
                        for (MData mData : dbScreen) {
                            String s = mData.getValues();
                            JSONObject jsonObject1 = new JSONObject(s);
                            Log.e("values", s);
                            screenArrayList.add(jsonObject1);
                        }
                    }
                    if (screenArrayList.size() > 0) {
                        try {
                            jsonObject = new JSONObject();
                            jsonObject.put("appId", SharedPref.getInstance().getStringValue(context, context.getString(R.string.resulticksSharedAPIKey)));
                            jsonObject.put("campaignId", SharedPref.getInstance().getStringValue(context, context.getString(R.string.resulticksSharedCampaignId)));
                            jsonObject.put("userId", SharedPref.getInstance().getStringValue(context, context.getString(R.string.resulticksSharedUserId)));
                            jsonObject.put("deviceId", SharedPref.getInstance().getStringValue(context, context.getString(R.string.resulticksSharedDatabaseDeviceId)));
                            jsonObject.put("screen", new JSONArray(screenArrayList));

                        } catch (Exception e) {
                            ExceptionTracker.track(e);
                        }
                    }
                }
            } catch (Exception e) {
                ExceptionTracker.track(e);
            }

            if (jsonObject != null)
                return jsonObject.toString();
            else return null;

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null)
                apiCallScreenTracking(result);


        }


    }

}
