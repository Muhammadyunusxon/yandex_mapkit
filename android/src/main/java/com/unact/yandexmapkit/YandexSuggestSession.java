package com.unact.yandexmapkit;

import androidx.annotation.NonNull;

import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SuggestItem;
import com.yandex.mapkit.search.SuggestResponse;
import com.yandex.mapkit.search.SuggestSession;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;

public class YandexSuggestSession implements MethodChannel.MethodCallHandler {
  private final int id;
  private SuggestSession session;
  private final MethodChannel methodChannel;
  private final SearchManager searchManager;
  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
  private static final Map<Integer, YandexSuggestSession> suggestSessions  = new HashMap<>();

  public static void initSession(int id, BinaryMessenger messenger, SearchManager searchManager) {
    suggestSessions.put(id, new YandexSuggestSession(id, messenger, searchManager));
  }

  public YandexSuggestSession(int id, BinaryMessenger messenger, SearchManager searchManager) {
    this.id = id;
    this.searchManager = searchManager;

    methodChannel = new MethodChannel(messenger, "yandex_mapkit/yandex_suggest_session_" + id);
    methodChannel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "getSuggestions":
        getSuggestions(call, result);
        break;
      case "reset":
        reset();
        result.success(null);
        break;
      case "close":
        close();
        result.success(null);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  public void reset() {
    session.reset();
  }

  public void close() {
    session.reset();
    methodChannel.setMethodCallHandler(null);

    suggestSessions.remove(id);
  }

  @SuppressWarnings({"unchecked", "ConstantConditions"})
  public void getSuggestions(MethodCall call, Result result) {
    YandexSuggestSession self = this;
    Map<String, Object> params = ((Map<String, Object>) call.arguments);

    session = searchManager.createSuggestSession();
    session.suggest(
      (String) params.get("text"),
      Utils.boundingBoxFromJson((Map<String, Object>) params.get("boundingBox")),
      Utils.suggestOptionsFromJson((Map<String, Object>) params.get("suggestOptions")),
      new SuggestSession.SuggestListener() {
        @Override
        public void onResponse(@NonNull SuggestResponse response) { self.onResponse(response, result); }
        @Override
        public void onError(@NonNull Error error) { self.onError(error, result); }
      }
    );
  }

  private void onResponse(@NonNull SuggestResponse response, @NonNull Result result) {
    List<Map<String, Object>> suggests = new ArrayList<>();

    for (SuggestItem suggestItem : response.getItems()) {
      Map<String, Object> suggestMap = new HashMap<>();

      suggestMap.put("title", suggestItem.getTitle().getText());
      if (suggestItem.getSubtitle() != null) {
        suggestMap.put("subtitle", suggestItem.getSubtitle().getText());
      }
      suggestMap.put("displayText", suggestItem.getDisplayText());
      suggestMap.put("searchText", suggestItem.getSearchText());
      suggestMap.put("type", suggestItem.getType().ordinal());
      suggestMap.put("tags", suggestItem.getTags());
      suggestMap.put("center", suggestItem.getCenter() != null ? Utils.pointToJson(suggestItem.getCenter()) : null);

      suggests.add(suggestMap);
    }

    Map<String, Object> arguments = new HashMap<>();
    arguments.put("items", suggests);
    result.success(arguments);
  }

  private void onError(@NonNull Error error, @NonNull Result result) {
    result.success(Utils.errorToJson(error));
  }
}
