# nexus deploy rule

_xml_filetype = FileType([".xml"])
_jar_filetype = FileType([".jar"])


def _impl(ctx):
  """Upload a jar file to a Sonatype Nexus Maven Repository Server."""

  targetFile = ctx.file.file
  upload = ctx.outputs.upload

  pomFile = ctx.file.pomFile
  user = ctx.attr.user
  password = ctx.attr.password
  repository = ctx.attr.repository
  server = ctx.attr.server

  cmd = "curl -v -F r=%s -F hasPom=true -F e=jar -F file=@%s -F file=@%s -u %s:%s %sservice/local/artifact/maven/content > %s" % (
     repository, pomFile.path, targetFile.path, user, password, server, upload.path)

  print(cmd)

  ctx.action(
    inputs=[targetFile, pomFile],
    outputs=[upload],
    command=cmd,
    progress_message="Uploading to nexus %s" % (server),
    use_default_shell_env = True
    )

nexus_upload_jar = rule(
  implementation=_impl,
  attrs={
    "pomFile": attr.label(mandatory=True, allow_files=True, single_file=True),
    "file": attr.label(mandatory=True, allow_files=True, single_file=True),
    "user": attr.string(),
    "password": attr.string(),
    "repository": attr.string(),
    "server": attr.string(),
  },
  outputs={"upload": "%{name}-upload-results.txt"},
  )
