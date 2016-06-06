# encoding: utf-8
require 'spec_helper'
require "logstash/filters/dissect"

describe LogStash::Filters::Dissect do
  class LoggerMock
    attr_reader :msgs, :hashes
    def initialize()
      @msgs = []
      @hashes = []
    end

    def error(*msg)
      @msgs.push(msg[0])
      @hashes.push(msg[1])
    end

    def warn(*msg)
      @msgs.push(msg[0])
      @hashes.push(msg[1])
    end

    def debug?() true; end

    def debug(*msg)
      @msgs.push(msg[0])
      @hashes.push(msg[1])
    end
  end

  describe "Basic dissection" do
    let(:config) do <<-CONFIG
      filter {
        dissect {
          mapping => {
            message => "[%{occurred_at}] %{code} %{service} %{ic} %{svc_message}"
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
    let(:loggr)      { LoggerMock.new }

    before(:each) do
      filter.logger = loggr
      filter.register
      filter.filter(event)
    end

    it "dissect failure tag is added" do
      expect(loggr.msgs).to eq(["Event before dissection", "Dissector mapping, key not found in event", "Event after dissection"])
      expect(loggr.hashes[0]).to be_a(Hash)
      expect(loggr.hashes[1]).to be_a(Hash)
      expect(loggr.hashes[2]).to be_a(Hash)
      expect(loggr.hashes[1].keys).to include("key")
      expect(loggr.hashes[1]["key"]).to eq("blah-di-blah")
    end
  end

  describe "baseline performance test", :performance => true do
    event_count = 1000000
    min_rate = 30000

    max_duration = event_count / min_rate
    cfg_base = <<-CONFIG
          input {
            generator {
              count => #{event_count}
              message => "Mar 16 00:01:25 evita postfix/smtpd[1713]: connect from camomile.cloud9.net[168.100.1.3]"
            }
          }
          output { null { } }
        CONFIG

    config(cfg_base)
    start = Time.now.to_f
    agent do
      duration = (Time.now.to_f - start)
      puts "\n\ninputs/generator baseline rate: #{"%02.0f/sec" % (event_count / duration)}, elapsed: #{duration}s\n\n"
      insist { duration } < max_duration
    end
  end

  describe "dissect performance test", :performance => true do
    event_count = 1000000
    min_rate = 30000
    max_duration = event_count / min_rate

    cfg_filter = <<-CONFIG
          input {
            generator {
              count => #{event_count}
              message => "Mar 16 00:01:25 evita postfix/smtpd[1713]: connect from camomile.cloud9.net[168.100.1.3]"
            }
          }
          filter {
            dissect {
              mapping => {
                "message" => "%{timestamp} %{+timestamp} %{+timestamp} %{logsource} %{program}[%{pid}]: %{msg}"
              }
            }
          }
          output { null { } }
        CONFIG

    config(cfg_filter)
    start = Time.now.to_f
    agent do
      duration = (Time.now.to_f - start)
      puts "\n\nfilters/dissect rate: #{"%02.0f/sec" % (event_count / duration)}, elapsed: #{duration}s\n\n"
      insist { duration } < event_count / min_rate
    end
  end
end
