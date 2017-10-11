require "logstash/devutils/rake"

require 'rubygems'
require 'rubygems/package_task'
require 'fileutils'
require 'rspec/core/rake_task'

# Please read BUILD_INSTRUCTIONS.md

desc "Compile and vendor java into ruby"
task :vendor => [:bundle_install] do
  exit(1) unless system './gradlew check vendor'
  puts "-------------------> vendored dissect jar via rake"
end

desc "Compile and vendor java into ruby for travis, its done bundle install already"
task :travis_vendor => [:write_gradle_properties] do
  exit(1) unless system './gradlew check vendor'
  puts "-------------------> vendored dissect jar via rake"
end

desc "Do bundle install and write gradle.properties"
task :bundle_install do
  `bundle install`
  delete_create_gradle_properties
end

desc "Write gradle.properties" # used by travis
task :write_gradle_properties do
  delete_create_gradle_properties
end

spec = Gem::Specification.load('logstash-filter-dissect.gemspec')
Gem::PackageTask.new(spec) do
  desc 'Package gem'
  task :package => [:vendor]
end

RSpec::Core::RakeTask.new(:spec)

task :check => [:vendor, :spec]

task :travis_test => [:travis_vendor, :spec]

desc "Run vendor with custom Logstash path"
task :custom_ls_check, :ls_dir do |task, args|
  ls_path = args[:ls_dir]
  system(custom_ls_path_shell_script(ls_path))
end

def custom_ls_path_shell_script(path)
  <<TXT
export LOGSTASH_PATH='#{path}'
export LOGSTASH_SOURCE=1
bundle install
bundle exec rake travis_vendor
bundle exec rspec spec
TXT
end

def delete_create_gradle_properties
  root_dir = File.dirname(__FILE__)
  gradle_properties_file = "#{root_dir}/gradle.properties"
  lsc_path = `bundle show logstash-core`.split(/\n/).first

  FileUtils.rm_f(gradle_properties_file)
  File.open(gradle_properties_file, "w") do |f|
    f.puts "logstashCoreGemPath=#{lsc_path}"
  end
  puts "-------------------> Wrote #{gradle_properties_file}"
  puts `cat #{gradle_properties_file}`
end
