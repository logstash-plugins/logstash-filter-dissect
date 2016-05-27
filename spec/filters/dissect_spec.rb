# encoding: utf-8
require 'spec_helper'
require "logstash/filters/dissect"

describe LogStash::Filters::Dissect do
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
end
