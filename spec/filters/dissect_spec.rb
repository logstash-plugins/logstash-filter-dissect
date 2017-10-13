# encoding: utf-8
require 'spec_helper'
require "logstash/filters/dissect"

describe LogStash::Filters::Dissect do

  describe "Basic dissection" do
    let(:config) do <<-CONFIG
      filter {
        dissect {
          mapping => {
            message => "[%{occurred_at}] %{code} %{service->} %{ic} %{svc_message}"
          }
        }
      }
    CONFIG
    end

    sample("message" => "[25/05/16 09:10:38:425 BST] 00000001 SystemOut     O java.lang:type=MemoryPool,name=class storage") do
      expect(subject.get("occurred_at")).to eq("25/05/16 09:10:38:425 BST")
      expect(subject.get("code")).to eq("00000001")
      expect(subject.get("service")).to eq("SystemOut")
      expect(subject.get("ic")).to eq("O")
      expect(subject.get("svc_message")).to eq("java.lang:type=MemoryPool,name=class storage")
      expect(subject.get("tags")).to be_nil
    end
  end

  describe "Basic dissection, like CSV with missing fields" do
    let(:config) do <<-CONFIG
      filter {
        dissect {
          mapping => {
            message => '[%{occurred_at}] %{code} %{service} values: "%{v1}","%{v2}","%{v3}"%{rest}'
          }
        }
      }
    CONFIG
    end

    sample("message" => '[25/05/16 09:10:38:425 BST] 00000001 SystemOut values: "f1","","f3"') do
      expect(subject.get("occurred_at")).to eq("25/05/16 09:10:38:425 BST")
      expect(subject.get("code")).to eq("00000001")
      expect(subject.get("service")).to eq("SystemOut")
      expect(subject.get("v1")).to eq("f1")
      expect(subject.get("v2")).to eq("")
      expect(subject.get("v3")).to eq("f3")
      expect(subject.get("rest")).to eq("")
      expect(subject.get("tags")).to be_nil
    end
  end

  describe "Basic dissection with datatype conversion" do
    let(:config) do <<-CONFIG
      filter {
        dissect {
          mapping => {
            message => "[%{occurred_at}] %{code} %{service} %{?ic}=%{&ic}% %{svc_message}"
          }
          convert_datatype => {
            cpu => "float"
            code => "int"
          }
        }
      }
    CONFIG
    end

    sample("message" => "[25/05/16 09:10:38:425 BST] 00000001 SystemOut cpu=95.43% java.lang:type=MemoryPool,name=class storage") do
      expect(subject.get("occurred_at")).to eq("25/05/16 09:10:38:425 BST")
      expect(subject.get("code")).to eq(1)
      expect(subject.get("service")).to eq("SystemOut")
      expect(subject.get("cpu")).to eq(95.43)
      expect(subject.get("svc_message")).to eq("java.lang:type=MemoryPool,name=class storage")
    end
  end

  describe "Basic dissection with multibyte Unicode characters" do
    let(:config) do <<-CONFIG
      filter {
        dissect {
          mapping => {
            message => "[%{occurred_at}]྿྿྿%{code}྿%{service}྿྿྿྿%{?ic}=%{&ic}%྿྿%{svc_message}"
          }
          convert_datatype => {
            cpu => "float"
            code => "int"
          }
        }
      }
    CONFIG
    end

    sample("message" => "[25/05/16 09:10:38:425 BST]྿྿྿00000001྿SystemOut྿྿྿྿cpu=95.43%྿྿java.lang:type=MemoryPool,name=class storage") do
      expect(subject.get("occurred_at")).to eq("25/05/16 09:10:38:425 BST")
      expect(subject.get("code")).to eq(1)
      expect(subject.get("service")).to eq("SystemOut")
      expect(subject.get("cpu")).to eq(95.43)
      expect(subject.get("svc_message")).to eq("java.lang:type=MemoryPool,name=class storage")
    end
  end

  describe "Basic dissection with failing datatype conversion" do
    subject(:filter) {  LogStash::Filters::Dissect.new(config)  }

    let(:message)    { "[25/05/16 09:10:38:425 BST] 00000001 SystemOut cpu=95.43% java.lang:type=MemoryPool,name=class storage" }
    let(:config)     do
      {
          "mapping" => {"message" => "[%{occurred_at}] %{code} %{service} %{?ic}=%{&ic}% %{svc_message}"},
          "convert_datatype" => {
            "ccu" => "float", # ccu field -> nil
            "other" => "int" # other field -> hash - not coercible
          }
      }
    end
    let(:event)      { LogStash::Event.new("message" => message, "other" => {}) }

    it "tags and log messages are created" do
      filter.register
      filter.filter(event)
      expect(event.get("code")).to eq("00000001")
      tags = event.get("tags")
      expect(tags).to include("_dataconversionnullvalue_ccu_float")
      expect(tags).to include("_dataconversionuncoercible_other_int")
      # Logging moved to java can't mock ruby logger anymore
    end
  end

  describe "Basic dissection when the source field does not exist" do
    let(:event) { LogStash::Event.new("message" => "foo", "other" => {}) }
    subject(:filter) {  LogStash::Filters::Dissect.new(config)  }
    let(:config)     do
      {
        "mapping" => {"msg" => "[%{occurred_at}] %{code} %{service} %{?ic}=%{&ic}% %{svc_message}"},
      }
    end
    it "does not raise an error" do
      filter.register
      expect(subject).to receive(:metric_increment).once.with(:failures)
      # it should log a warning, but we can't test that
      expect { filter.filter(event) }.not_to raise_exception
    end
  end

  describe "Invalid datatype conversion specified integer instead of int" do
    subject(:filter) {  LogStash::Filters::Dissect.new(config)  }
    let(:config)     do
      {
        "convert_datatype" => {
          "code" => "integer", # only int is supported
        }
      }
    end
    it "raises an error" do
      expect { filter.register }.to raise_exception(LogStash::ConvertDatatypeFormatError)
    end
  end

  describe "Integer datatype conversion, handle large integers" do
    let(:config) do <<-CONFIG
      filter {
        dissect {
          convert_datatype => {
            "big_number" => "int"
          }
        }
      }
    CONFIG
    end
    sample("big_number" => "4394740425750718628") do
      expect(subject.get("big_number")).to eq(4394740425750718628)
    end
  end

  describe "Float datatype conversion, handle large floats" do
    let(:config) do <<-CONFIG
      filter {
        dissect {
          convert_datatype => {
            "big_number" => "float"
          }
        }
      }
    CONFIG
    end
    sample("big_number" => "4394740425750718628.345324") do
      expect(subject.get("big_number")).to eq(4394740425750718628.345324)
    end
  end

  describe "dissect with skip and append" do
    let(:config) do <<-CONFIG
        filter {
          dissect {
            mapping => {
              "message" => "%{timestamp} %{+timestamp} %{+timestamp} %{logsource} %{} %{program}[%{pid}]: %{msg}"
            }
            add_field => { favorite_filter => "why, dissect of course" }
          }
        }
      CONFIG
    end

    sample("message" => "Mar 16 00:01:25 evita skip-this postfix/smtpd[1713]: connect from camomile.cloud9.net[168.100.1.3]") do
      expect(subject.get("tags")).to be_nil
      expect(subject.get("logsource")).to eq("evita")
      expect(subject.get("timestamp")).to eq("Mar 16 00:01:25")
      expect(subject.get("msg")).to eq("connect from camomile.cloud9.net[168.100.1.3]")
      expect(subject.get("program")).to eq("postfix/smtpd")
      expect(subject.get("pid")).to eq("1713")
      expect(subject.get("favorite_filter")).to eq("why, dissect of course")
    end
  end

  context "when mapping a key is not found" do
    subject(:filter) {  LogStash::Filters::Dissect.new(config)  }

    let(:message)    { "very random message :-)" }
    let(:config)     { {"mapping" => {"blah-di-blah" => "%{timestamp} %{+timestamp}"}} }
    let(:event)      { LogStash::Event.new("message" => message) }

    it "does not raise any exceptions" do
      expect{filter.register}.not_to raise_exception
    end

    it "dissect failure key missing is logged" do
      filter.register
      expect{filter.filter(event)}.not_to raise_exception
      # Logging moved to java, can't Mock Logger
    end
  end

  describe "valid field format handling" do
    subject(:filter) {  LogStash::Filters::Dissect.new(config)  }
    let(:config)     { {"mapping" => {"message" => "%{+timestamp/2} %{+timestamp/1} %{?no_name} %{&no_name} %{} %{program}[%{pid}]: %{msg}"}}}

    it "does not raise an error in register" do
      expect{filter.register}.not_to raise_exception
    end
  end

  describe "invalid field format handling" do
    subject(:filter) {  LogStash::Filters::Dissect.new(config)  }

    context "when field is defined as Append and Indirect (+&)" do
      let(:config)     { {"mapping" => {"message" => "%{+&timestamp}"}}}
      it "raises an error in register" do
        msg = /\Aorg\.logstash\.dissect\.fields\.InvalidFieldException: Field cannot prefix with both Append and Indirect Prefix .+/
        expect{filter.register}.to raise_exception(LogStash::FieldFormatError, msg)
      end
    end

    context "when field is defined as Indirect and Append (&+)" do
      let(:config)     { {"mapping" => {"message" => "%{&+timestamp}"}}}
      it "raises an error in register" do
        msg = /\Aorg\.logstash\.dissect\.fields\.InvalidFieldException: Field cannot prefix with both Append and Indirect Prefix .+/
        expect{filter.register}.to raise_exception(LogStash::FieldFormatError, msg)
      end
    end
  end

  describe "metrics tracking" do

    let(:options) { { "mapping" => { "message" => "%{a} %{b}" } } }
    subject { described_class.new(options) }

    before(:each) { subject.register }

    context "when match is successful" do
      let(:event) { LogStash::Event.new("message" => "1 2") }

      it "should increment the matches metric" do
        expect(subject).to receive(:metric_increment).once.with(:matches)
        subject.filter(event)
      end
    end

    context "when match is not successful" do
      let(:event) { LogStash::Event.new("message" => "") }

      it "should increment the failures metric" do
        expect(subject).to receive(:metric_increment).once.with(:failures)
        subject.filter(event)
      end
    end
  end

  describe "When the delimiters contain '{' and '}'" do
    let(:options) { { "mapping" => { "message" => "{%{a}}{%{b}}%{rest}" } } }
    subject { described_class.new(options) }
    let(:event) { LogStash::Event.new({ "message" => "{foo}{bar}" }) }
    before(:each) do
      subject.register
      subject.filter(event)
    end
    it "should dissect properly and not add tags to the event" do
      expect(event.get("a")).to eq("foo")
      expect(event.get("b")).to eq("bar")
      expect(event.get("rest")).to eq("")
      expect(event.get("tags")).to be_nil
    end
  end

  describe "Basic dissection" do

    let(:options) { { "mapping" => { "message" => "%{a} %{b}" } } }
    subject { described_class.new(options) }
    let(:event) { LogStash::Event.new(event_data) }

    before(:each) do
      subject.register
      subject.filter(event)
    end

    context "when no field" do
      let(:event_data) { {} }
      it "should not add tags to the event" do
        expect(event.get("tags")).to be_nil
      end
    end

    context "when field is empty" do
      let(:event_data) { { "message" => "" } }
      it "should add tags to the event" do
        tags = event.get("tags")
        expect(tags).not_to be_nil
        expect(tags).to include("_dissectfailure")
      end
    end
  end
end
