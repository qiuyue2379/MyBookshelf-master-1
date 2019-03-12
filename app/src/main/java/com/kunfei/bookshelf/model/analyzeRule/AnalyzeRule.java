package com.kunfei.bookshelf.model.analyzeRule;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.kunfei.bookshelf.base.BaseModelImpl;
import com.kunfei.bookshelf.bean.BaseBookBean;
import com.kunfei.bookshelf.model.impl.IHttpGetApi;
import com.kunfei.bookshelf.utils.NetworkUtil;
import com.kunfei.bookshelf.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import retrofit2.Call;

import static com.kunfei.bookshelf.constant.AppConstant.MAP_STRING;


/**
 * Created by REFGD.
 * 统一解析接口
 */
public class AnalyzeRule {
    private static final Pattern putPattern = Pattern.compile("@put:\\{.+?\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern getPattern = Pattern.compile("@get:\\{.+?\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern jsPattern = Pattern.compile("(<js>[\\w\\W]*?</js>|@js:[\\w\\W]*$)", Pattern.CASE_INSENSITIVE);

    private BaseBookBean book;
    private Object _object;
    private Boolean _isJSON;

    private AnalyzeByXPath analyzeByXPath = null;
    private AnalyzeByJSoup analyzeByJSoup = null;
    private AnalyzeByJSonPath analyzeByJSonPath = null;

    private boolean objectChangedXP = false;
    private boolean objectChangedJS = false;
    private boolean objectChangedJP = false;

    public AnalyzeRule(BaseBookBean bookBean) {
        book = bookBean;
    }

    public void setBook(BaseBookBean book) {
        this.book = book;
    }

    public AnalyzeRule setContent(String body) {
        if (body == null) throw new AssertionError("Content cannot be null");
        _isJSON = StringUtils.isJsonType(body);
        _object = body;
        objectChangedXP = true;
        objectChangedJS = true;
        objectChangedJP = true;
        return this;
    }

    public AnalyzeRule setContent(Object object, boolean isJSON) {
        _object = object;
        _isJSON = isJSON;
        objectChangedXP = true;
        objectChangedJS = true;
        objectChangedJP = true;
        return this;
    }

    /**
     * XPath
     */
    private AnalyzeByXPath getAnalyzeByXPath(Object o) {
        if (o != null) {
            return new AnalyzeByXPath().parse(o.toString());
        }
        return getAnalyzeByXPath();
    }

    private AnalyzeByXPath getAnalyzeByXPath() {
        if (analyzeByXPath == null || objectChangedXP) {
            analyzeByXPath = new AnalyzeByXPath();
            analyzeByXPath.parse(_object.toString());
            objectChangedXP = false;
        }
        return analyzeByXPath;
    }

    /**
     * JSOUP
     */
    private AnalyzeByJSoup getAnalyzeByJSoup(Object o) {
        if (o != null) {
            return new AnalyzeByJSoup().parse(o.toString());
        }
        return getAnalyzeByJSoup();
    }

    private AnalyzeByJSoup getAnalyzeByJSoup() {
        if (analyzeByJSoup == null || objectChangedJS) {
            analyzeByJSoup = new AnalyzeByJSoup();
            analyzeByJSoup.parse(_object);
            objectChangedJS = false;
        }
        return analyzeByJSoup;
    }

    /**
     * JSON
     */
    private AnalyzeByJSonPath getAnalyzeByJSonPath(Object o) {
        if (o != null) {
            if (o instanceof String) {
                return new AnalyzeByJSonPath().parse(o.toString());
            }
            return new AnalyzeByJSonPath().parse(o);
        }
        return getAnalyzeByJSonPath();
    }

    private AnalyzeByJSonPath getAnalyzeByJSonPath() {
        if (analyzeByJSonPath == null || objectChangedJP) {
            analyzeByJSonPath = new AnalyzeByJSonPath();
            if (_object instanceof String) {
                analyzeByJSonPath.parse(String.valueOf(_object));
            } else {
                analyzeByJSonPath.parse(_object);
            }
            objectChangedJP = false;
        }
        return analyzeByJSonPath;
    }

    /**
     * 获取文本列表
     */
    public List<String> getStringList(String rule) throws ScriptException {
        return getStringList(rule, null);
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String ruleStr, String baseUrl) throws ScriptException {
        Object result = null;
        List<SourceRule> ruleList = splitSourceRule(ruleStr);
        for (SourceRule rule : ruleList) {
            switch (rule.mode) {
                case Js:
                    if (result == null) result = String.valueOf(_object);
                    result = evalJS(rule.rule, result, baseUrl);
                    break;
                case JSon:
                    result = getAnalyzeByJSonPath(result).readStringList(rule.rule);
                    break;
                case XPath:
                    result = getAnalyzeByXPath(result).getStringList(rule.rule);
                    break;
                default:
                    result = getAnalyzeByJSoup(result).getAllResultList(rule.rule);
            }
        }
        if (result != null && !StringUtils.isTrimEmpty(baseUrl)) {
            List<String> urlList = new ArrayList<>();
            for (String url : (List<String>) result) {
                url = NetworkUtil.getAbsoluteURL(baseUrl, url);
                if (!urlList.contains(url)) {
                    urlList.add(url);
                }
            }
            return urlList;
        }
        if (result == null) return new ArrayList<>();
        return (List<String>) result;
    }

    /**
     * 获取文本
     */
    public String getString(String rule) throws ScriptException {
        return getString(rule, null);
    }

    public String getString(String ruleStr, String _baseUrl) throws ScriptException {
        if (StringUtils.isTrimEmpty(ruleStr)) {
            return null;
        }
        String result = null;
        List<SourceRule> ruleList = splitSourceRule(ruleStr);
        for (SourceRule rule : ruleList) {
            if (!StringUtils.isTrimEmpty(rule.rule)) {
                switch (rule.mode) {
                    case Js:
                        if (result == null) result = String.valueOf(_object);
                        result = (String) evalJS(rule.rule, result, _baseUrl);
                        break;
                    case JSon:
                        result = getAnalyzeByJSonPath(result).read(rule.rule);
                        break;
                    case XPath:
                        result = getAnalyzeByXPath(result).getString(rule.rule, _baseUrl);
                        break;
                    case Default:
                        if (TextUtils.isEmpty(_baseUrl)) {
                            result = getAnalyzeByJSoup(result).getResult(rule.rule);
                        } else {
                            result = getAnalyzeByJSoup(result).getResultUrl(rule.rule);
                        }
                }
            }
        }
        if (!StringUtils.isTrimEmpty(_baseUrl)) {
            result = NetworkUtil.getAbsoluteURL(_baseUrl, result);
        }
        return result;
    }

    /**
     * 获取列表
     */
    public AnalyzeCollection getElements(String ruleStr) throws ScriptException {
        Object result = null;
        AnalyzeCollection collection = null;
        List<SourceRule> ruleList = splitSourceRule(ruleStr);
        for (SourceRule rule : ruleList) {
            switch (rule.mode) {
                case Js:
                    if (result == null) result = String.valueOf(_object);
                    result = evalJS(rule.rule, result, null);
                    break;
                case JSon:
                    collection = new AnalyzeCollection(getAnalyzeByJSonPath(result).readList(rule.rule), true);
                    break;
                case XPath:
                    collection = new AnalyzeCollection(getAnalyzeByXPath(result).getElements(rule.rule));
                    break;
                default:
                    collection = new AnalyzeCollection(getAnalyzeByJSoup(result).getElements(rule.rule));
            }
        }
        return collection;
    }

    /**
     * 保存变量
     */
    private void analyzeVariable(Map<String, String> putVariable) throws ScriptException {
        for (Map.Entry<String, String> entry : putVariable.entrySet()) {
            if (book != null) {
                book.putVariable(entry.getKey(), getString(entry.getValue()));
            }
        }
    }

    /**
     * 分解规则生成规则列表
     */
    private List<SourceRule> splitSourceRule(String ruleStr) {
        List<SourceRule> ruleList = new ArrayList<>();
        if (ruleStr == null) return ruleList;
        Mode mode;
        if (StringUtils.startWithIgnoreCase(ruleStr, "@XPath:")) {
            mode = Mode.XPath;
            ruleStr = ruleStr.substring(7);
        } else if (StringUtils.startWithIgnoreCase(ruleStr, "@JSon:")) {
            mode = Mode.JSon;
            ruleStr = ruleStr.substring(6);
        } else {
            if (_isJSON) {
                mode = Mode.JSon;
            } else {
                mode = Mode.Default;
            }
        }
        //分离put规则
        Matcher putMatcher = putPattern.matcher(ruleStr);
        if (putMatcher.find()) {
            String find = putMatcher.group(0);
            ruleStr = ruleStr.replace(find, "");
            find = find.substring(5);
            try {
                Map<String, String> putVariable = new Gson().fromJson(find, MAP_STRING);
                analyzeVariable(putVariable);
            } catch (Exception ignored) {
            }
        }
        //替换get值
        Matcher getMatcher = getPattern.matcher(ruleStr);
        while (getMatcher.find()) {
            String find = getMatcher.group();
            String value = "";
            if (book != null && book.getVariableMap() != null) {
                value = book.getVariableMap().get(find.substring(6, find.length() - 1));
                if (value == null) value = "";
            }
            ruleStr = ruleStr.replace(find, value);
        }
        int start = 0;
        Matcher jsMatcher = jsPattern.matcher(ruleStr);
        while (jsMatcher.find()) {
            if (jsMatcher.start() > start) {
                ruleList.add(new SourceRule(ruleStr.substring(start, jsMatcher.start()), mode));
            }
            ruleList.add(new SourceRule(jsMatcher.group(), Mode.Js));
            start = jsMatcher.end();
        }
        if (ruleStr.length() > start) {
            ruleList.add(new SourceRule(ruleStr.substring(start), mode));
        }
        return ruleList;
    }

    /**
     * 规则类
     */
    private class SourceRule {
        Mode mode;
        String rule;

        SourceRule(String ruleStr, Mode mainMode) {
            this.mode = mainMode;
            if (mode == Mode.Js) {
                if (ruleStr.startsWith("<")) {
                    rule = ruleStr.substring(4, ruleStr.lastIndexOf("<"));
                } else {
                    rule = ruleStr.substring(4);
                }
            } else {
                if (StringUtils.startWithIgnoreCase(ruleStr, "@XPath:")) {
                    mode = Mode.XPath;
                    rule = ruleStr.substring(7);
                } else if (StringUtils.startWithIgnoreCase(ruleStr, "//")) {//XPath特征很明显,无需配置单独的识别标头
                    mode = Mode.XPath;
                    rule = ruleStr;
                } else if (StringUtils.startWithIgnoreCase(ruleStr, "@JSon:")) {
                    mode = Mode.JSon;
                    rule = ruleStr.substring(6);
                } else {
                    rule = ruleStr;
                }
            }
        }

    }

    private enum Mode {
        XPath, JSon, Default, Js
    }

    private static class EngineHelper {
        private static final ScriptEngine INSTANCE = new ScriptEngineManager().getEngineByName("rhino");
    }

    /**
     * 执行JS
     */
    private Object evalJS(String jsStr, Object result, String baseUrl) throws ScriptException {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("java", this);
        bindings.put("result", result);
        bindings.put("baseUrl", baseUrl);
        return EngineHelper.INSTANCE.eval(jsStr, bindings);
    }

    /**
     * js实现跨域访问,不能删
     */
    @SuppressWarnings("unused")
    public String ajax(String urlStr) {
        try {
            Call<String> call = BaseModelImpl.getInstance().getRetrofitString(StringUtils.getBaseUrl(urlStr))
                    .create(IHttpGetApi.class).getWebContentCall(urlStr, new HashMap<>());
            return call.execute().body();
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }

    /**
     * js实现解码,不能删
     */
    @SuppressWarnings("unused")
    public String base64Decoder(String base64) {
        return StringUtils.base64Decode(base64);
    }
}