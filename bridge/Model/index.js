import { assocPath, construct, merge, pathOr } from "ramda";
import iRequest from "superagent";
const prefix = require("superagent-prefix")("/static");

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
    { response: 30000, deadline: 30000 },
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

    return iRequest
      .get(url)
      .set(headers)
      .timeout(timeup.response)
      .use(prefix)
      .query(params);
  },
  post: function (url) {
    const { headers, body, timeup } = this;

    return iRequest
      .post(url)
      .set(headers)
      .timeout(timeup.response)
      .use(prefix)
      .send(body);
  },
  field: function (body) {
    const { headers, timeup, os, url, files } = this;
    var req = iRequest
      .post(url)
      .set(headers)
      .timeout(timeup.response)
      .use(prefix);
    files.map((file, i) => {
      req.attach(`file ${i}`, file.uri);
    });
    return req.field("dataSet", body);
  },
  put: function (url) {
    const { headers, body, timeup } = this;

    return iRequest
      .put(url)
      .set(headers)
      .timeout(timeup.response)
      .use(prefix)
      .send(body);
  },
  delete: function (url) {
    const { headers, timeup } = this;

    return iRequest
      .delete(url)
      .set(headers)
      .timeout(timeup.response)
      .use(prefix);
  },
  getInflate: function (url) {
    const { headers, params, os, timeup } = this;

    return iRequest
      .get(url)
      .set(headers)
      .use(prefix)
      .timeout(timeup.response)
      .query(params);
  },
  submitInflate: function (url) {
    const { headers, body, os, timeup } = this;

    return iRequest
      .post(url)
      .set(headers)
      .timeout(timeup.response)
      .use(prefix)
      .send(body);
  },
  putInflate: function (url) {
    const { headers, body, os, timeup } = this;

    return iRequest
      .put(url)
      .set(headers)
      .timeout(timeup.response)
      .use(prefix)
      .send(body);
  },
  deleteInflate: function (url) {
    const { headers, os, timeup } = this;

    return iRequest
      .delete(url)
      .set(headers)
      .timeout(timeup.response)
      .use(prefix);
  },
};

export default construct(IRModel);
