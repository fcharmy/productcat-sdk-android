package com.visenze.productcat.android.util;

import com.visenze.productcat.android.model.ResultList;
import com.visenze.productcat.android.ProductCatException;
import com.visenze.productcat.android.model.Box;
import com.visenze.productcat.android.model.ProductSummary;
import com.visenze.productcat.android.model.ProductType;
import com.visenze.productcat.android.model.Store;
import com.visenze.productcat.android.model.Tag;
import com.visenze.productcat.android.model.TagGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Parses ProductCat Search API JSON responses.
 */
public class ResponseParser {

    public static ResultList parseResult(String jsonResponse) {

        try {
            ResultList resultList = new ResultList();

            JSONObject resultObj = new JSONObject(jsonResponse);
            resultList.setErrorMessage(parseResponseError(resultObj));

            if (resultObj.has("reqid")) {
                resultList.setReqid(resultObj.getString("reqid"));
            }

            if (resultObj.has("recognize_result")) {
                JSONArray tagGroupArray = resultObj.getJSONArray("recognize_result") ;

                resultList.setTagGroups(parseTagGroups(tagGroupArray));

            }

            if (resultObj.has("result")) {
                JSONArray resultArray = resultObj.getJSONArray("result");
                resultList.setProductSummaryList(parseProductSummaryList(resultArray));
            }

            if (resultObj.has("product_types")) {
                JSONArray productTypeArray = resultObj.getJSONArray("product_types");
                resultList.setProductTypes(parseProductTypeList(productTypeArray));
            }

            if (resultObj.has("im_id")) {
                resultList.setImId(resultObj.getString("im_id"));
            }

            if (resultObj.has("ocr")) {
                resultList.setOcr(resultObj.getString("ocr"));
            }

            if (resultObj.has("ocr_debug")) {
                resultList.setOcrDebug(resultObj.getString("ocr_debug"));
            }

            if (resultObj.has("visearch_debug")) {
                resultList.setVisearchDebug(resultObj.getString("visearch_debug"));
            }

            return resultList;
        } catch (JSONException e) {
            throw new ProductCatException("Error parsing response " + e.getMessage(), e);
        }
    }

    private static List<TagGroup> parseTagGroups(JSONArray arr){
        List<TagGroup> groups = new ArrayList<TagGroup>();

        for(int i = 0, length = arr.length() ; i < length ; i++){
            JSONObject o = arr.optJSONObject(i);

            if(o!=null){
                TagGroup group = new TagGroup();
                group.tagGroup = o.optString("tag_group" , "");
                JSONArray tagsArr = o.optJSONArray("tags");
                if(group.tagGroup != null && tagsArr!=null){
                    for(int j = 0 , taglength = tagsArr.length(); j < taglength ; j++ ){
                        JSONObject tagObject = tagsArr.optJSONObject(j);
                        if(tagObject!=null){
                            Tag tag = new Tag();
                            tag.tagId = tagObject.optString("tag_id");
                            tag.tag = tagObject.optString("tag");
                            tag.score = tagObject.optDouble("score", 0);
                            tag.tagGroup = group.tagGroup;
                            group.tags.add(tag);

                        }
                    }

                    groups.add(group);
                }

            }

        }

        return  groups;
    }

    private static Map<String, String> parseQueryInfo(JSONObject qinfoObj) {
        Map<String, String> queryInfo = new HashMap<String, String>();
        try {
            Iterator<String> nameItr = qinfoObj.keys();
            while (nameItr.hasNext()) {
                String name = nameItr.next();
                queryInfo.put(name, qinfoObj.getString(name));
            }
        } catch (JSONException e) {
            throw new ProductCatException("JsonParse Error: " + e.toString());
        }

        return queryInfo;
    }

    private static List<ProductSummary> parseProductSummaryList(JSONArray resultArray) throws ProductCatException {
        List<ProductSummary> resultList = new ArrayList<ProductSummary>();
        int size = resultArray.length();

        try {
            for (int i = 0; i < size; i++) {
                JSONObject summary = resultArray.getJSONObject(i);
                ProductSummary productSummary = new ProductSummary();
                productSummary.setPgid(summary.getString("pgid"));
                if (summary.has("pid")) {
                    productSummary.setPid(summary.getString("pid"));
                }
                productSummary.setMainImage(summary.optString("main_image"));

                // images
                if ( summary.has("images") ) {
                    JSONArray imgArr = summary.getJSONArray("images");
                    for (int imgIndex = 0, length = imgArr.length() ; imgIndex < length ; imgIndex++) {
                        productSummary.getImages().add(imgArr.getString(imgIndex) );
                    }
                }

                productSummary.setTitle(summary.optString("title"));
                productSummary.setDesc(summary.optString("desc"));
                productSummary.setCountry(summary.optString("country"));
                productSummary.setMinPrice(summary.optDouble("min_price" , 0));
                productSummary.setMaxPrice(summary.optDouble("max_price", 0));
                productSummary.setPriceUnit(summary.optString("price_unit"));
                productSummary.setAvailability(summary.optInt("availability", 1));
                productSummary.setProductUrl(summary.optString("product_url"));

                if (summary.has("stores")) {
                    JSONArray storesJson = summary.getJSONArray("stores");
                    for (int si = 0, length = storesJson.length() ; si < length ; si++) {
                        JSONObject storeObj = storesJson.getJSONObject(si);
                        Store store = new Store();
                        store.setStoreId(storeObj.optInt("store_id", 0));
                        store.setName(storeObj.optString("name", ""));
                        store.setAvailability(storeObj.optInt("availability", 1));

                        productSummary.getStores().add(store);
                    }
                }

                resultList.add(productSummary);
            }
        } catch (JSONException e) {
            throw new ProductCatException("Error parsing response result " + e.getMessage(), e);
        }

        return resultList;
    }

    private static List<ProductType> parseProductTypeList(JSONArray resultArray) throws ProductCatException {
        List<ProductType> resultList = new ArrayList<ProductType>();
        int size = resultArray.length();

        try {
            for (int i = 0; i < size; i++) {
                JSONObject typeObj = resultArray.getJSONObject(i);
                ProductType productType = new ProductType();
                productType.setType(typeObj.getString("type"));
                productType.setScore(typeObj.getDouble("score"));
                JSONArray boxCoordinate = typeObj.getJSONArray("box");
                Box box = new Box(boxCoordinate.getInt(0),
                        boxCoordinate.getInt(1),
                        boxCoordinate.getInt(2),
                        boxCoordinate.getInt(3));
                productType.setBox(box);

                if (typeObj.has("attributes")) {
                    JSONObject attrsArray = typeObj.getJSONObject("attributes");
                    JSONArray attrsNames = attrsArray.names();
                    Map attrsMapList = new HashMap<>();
                    for (int j = 0; attrsNames != null && j < attrsNames.length(); j++) {
                        JSONArray attrsItems = (JSONArray) attrsArray.get((String)attrsNames.get(j));
                        List<String> attrs = new ArrayList<>();
                        for (int k = 0; k < attrsItems.length(); k++) {
                            attrs.add((String) attrsItems.get(k));
                        }
                        attrsMapList.put(attrsNames.get(j), attrs);
                    }
                    productType.setAttributeList(attrsMapList);
                }

                resultList.add(productType);
            }
        } catch (JSONException e) {
            throw new ProductCatException("Error parsing response result " + e.getMessage(),e);
        }

        return resultList;
    }

    private static String parseResponseError(JSONObject jsonObj) {
        try {
            String status = jsonObj.getString("status");
            if (status == null) {
                throw new ProductCatException("Error parsing response: status is null");
            } else {
                if (status.equals("OK")) {
                    return null;
                } else {
                    if (!jsonObj.has("error") || jsonObj.getJSONArray("error").length() == 0) {
                        return "Error parsing response: missing error";
                    } else {
                        return jsonObj.getJSONArray("error").get(0).toString();
                    }
                }
            }
        } catch (JSONException e) {
            throw new ProductCatException("Error parsing response " + e.getMessage(), e);
        }
    }
}
