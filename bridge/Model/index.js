import axios from "axios";
import { assocPath, merge, pathOr } from "ramda";

function flexibleMerge(record, key, value) {
  return typeof key === "object"
    ? merge(record, key)
    : merge(record, { [key]: value });
}

function IRModel(record) {
  this.os = pathOr("android", ["os"], record);
  this.headers = pathOr({}, ["headers"], record);
  this.body = pathOr({}, ["body"], record);
  this.params = pathOr({}, ["params"], record);
  this.url = pathOr(null, ["url"], record);
  this.timeup = pathOr(
    { response: 30000 },
    ["timeup"],
    record
  );
  this.files = pathOr([], ["files"], record);
}

IRModel.prototype = {
  fix: function (key, value) {
    return assocPath([key], value, this);
  },
  set: function (key, value) {
    const headers = flexibleMerge(this.headers, key, value);
    return assocPath(["headers"], headers, this);
  },
  platform: function (value) {
    return assocPath(["os"], value, this);
  },
  query: function (key, value) {
    const params = flexibleMerge(this.params, key, value);
    return assocPath(["params"], params, this);
  },
  send: function (key, value) {
    const body = flexibleMerge(this.body, key, value);
    return assocPath(["body"], body, this);
  },
  timeout: function (value) {
    return assocPath(["timeup"], value, this);
  },
  get: function (url) {
    const { headers, params, timeup } = this;
    return axios.get(url, {
      headers,
      params,
      timeout: timeup.response,
    });
  },
  post: function (url) {
    const { headers, body, timeup } = this;
    return axios.post(url, body, {
      headers,
      timeout: timeup.response,
    });
  },
  field: function (body) {
    const { headers, timeup, files, url } = this;
    const formData = new FormData();

    files.forEach((file, i) => {
      formData.append(`file_${i}`, file.uri);
    });
    formData.append("dataSet", body);

    return axios.post(url, formData, {
      headers: { ...headers, "Content-Type": "multipart/form-data" },
      timeout: timeup.response,
    });
  },
  put: function (url) {
    const { headers, body, timeup } = this;
    return axios.put(url, body, {
      headers,
      timeout: timeup.response,
    });
  },
  delete: function (url) {
    const { headers, timeup } = this;
    return axios.delete(url, {
      headers,
      timeout: timeup.response,
    });
  },
  getInflate: function (url) {
    const { headers, params, timeup } = this;
    return axios.get(url, {
      headers,
      params,
      timeout: timeup.response,
      responseType: "arraybuffer",
    });
  },
  submitInflate: function (url) {
    const { headers, body, timeup } = this;
    return axios.post(url, body, {
      headers,
      timeout: timeup.response,
      responseType: "arraybuffer",
    });
  },
  putInflate: function (url) {
    const { headers, body, timeup } = this;
    return axios.put(url, body, {
      headers,
      timeout: timeup.response,
      responseType: "arraybuffer",
    });
  },
  deleteInflate: function (url) {
    const { headers, timeup } = this;
    return axios.delete(url, {
      headers,
      timeout: timeup.response,
      responseType: "arraybuffer",
    });
  },
};

export default (record) => new IRModel(record);
