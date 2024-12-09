(ns reviewtool.base.urilib
  (:require [lambdaisland.uri :as liuri]))

;;; URL encoding. Not sure if this could fit into lambdaisland.uri. This extends it by
;;; list arguments and a few modificaiton functions. It is worth asking at lambdaisland.uri
;;; and perhaps creating a PR.

(def uri liuri/uri)

(defn param [url key]
  (key (liuri/query-map url)))

(comment
  (param "https://example.com:300/foo/bar" :a) 
  (param "https://example.com:300/foo/bar?a=3" :a)
  (param "https://example.com:300/foo/bar?a=3&a=3" :a))

(defn param-as-vector [url key]
  (let [param (param url key)]
    (cond (not param) param
          (vector? param) param
          :else [param])))

(comment
  (param-as-vector "http://example.com/foo/bar" :columns)
  (param-as-vector "http://example.com/foo/bar?columns=1" :columns)
  (param-as-vector "http://example.com/foo/bar?columns=1&columns=2" :columns))

(defn set-param [url key value]
  (liuri/assoc-query url key value))

(comment
  (set-param "http://example.com/foo/bar" :columns 7)
  (set-param "http://example.com/foo/bar?columns=1" :columns 7)
  (set-param "http://example.com/foo/bar?columns=1&columns=2" :columns 7))

(defn remove-param [url key]
  (set-param url key nil))

(comment
  (remove-param "http://example.com/foo/bar" :columns)
  (remove-param "http://example.com/foo/bar?columns=1" :columns)
  (remove-param "http://example.com/foo/bar?columns=1&columns=2" :columns)
  (remove-param "http://example.com/foo/bar?columns=1" :nnnn)
  (remove-param "http://example.com/foo/bar?columns=1&columns=2" :nnnn))

(defn add-to-param [url key value]
  (set-param url key (conj (param-as-vector url key) value)))

(comment
  (add-to-param "http://example.com/foo/bar" :columns 7)
  (add-to-param "http://example.com/foo/bar?columns=1" :columns 7)
  (add-to-param "http://example.com/foo/bar?columns=1&columns=2" :columns 7))

(defn remove-from-param [url key value]
  (set-param url key (remove #(= % value)
                             (param-as-vector url key))))

(comment
  (remove-from-param "http://example.com/foo/bar" :columns "1")
  (remove-from-param "http://example.com/foo/bar?columns=1" :columns "1")
  (remove-from-param "http://example.com/foo/bar?columns=2" :columns "1")
  (remove-from-param "http://example.com/foo/bar?columns=1&columns=2" :columns "1"))

(-> "http://example.com/foo/bar?columns=1&columns=2" liuri/uri
    (set-param :x 23)
    (remove-from-param :columns "1")
    (assoc :path "/test.html")
    str)
